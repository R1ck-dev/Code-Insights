// Monta o manifesto de proveniencia do corpus externo (IC-16) a partir dos logs locais da coleta.
//
// Uso: node scripts/corpus-externo/manifesto.mjs <pasta-corpus-externo> <criterio.md> <saida.json>
//
// O manifesto vai para o repositorio e NAO contem codigo de terceiro, valor de complexidade nem texto livre
// dos logs (campos `detalhe` e `duvida` ficam so no log local). Por caso aceito: plataforma, id, URL do
// problema, fontes do gabarito (so URL/titulo/data), consulta e posicao da busca, URL permanente da solucao,
// autor/repositorio, licenca declarada, commit, data de acesso, SHA-256 e linhas do arquivo.
import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync, existsSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const [, , pasta, criterio, saida] = process.argv;
if (!pasta || !criterio || !saida) {
  console.error('uso: node manifesto.mjs <pasta-corpus-externo> <criterio.md> <saida.json>');
  process.exit(2);
}

const sha = (arquivo) => createHash('sha256').update(readFileSync(arquivo)).digest('hex');
const lerLog = (linguagem) =>
  readFileSync(join(pasta, `coleta-${linguagem}.jsonl`), 'utf8').trim().split('\n').map((l) => JSON.parse(l));

function caso(l) {
  const s = l.solucao;
  return {
    ordem: l.ordem,
    plataforma: l.plataforma,
    id: l.id,
    urlProblema: l.url,
    fontesDoGabarito: (l.fontes ?? []).map((f) => ({ url: f.url, titulo: f.titulo, acesso: f.acesso })),
    vereditoDasFontes: l.veredito_fontes,
    busca: { consulta: l.busca?.consulta, data: l.busca?.data, posicaoEscolhida: l.busca?.posicao_escolhida },
    solucao: {
      urlPermanente: s.url_blob,
      repositorio: s.repo,
      autor: s.autor,
      licencaDeclarada: s.licenca,
      commit: s.commit,
      acesso: s.acesso,
      arquivoLocal: s.arquivo_local.split(/[\\/]/).pop(),
      sha256: s.sha256,
      linhas: s.linhas,
      comentarioDeComplexidade: Boolean(s.comentario_de_complexidade),
    },
  };
}

function porLinguagem(linguagem) {
  const log = lerLog(linguagem);
  const aceitos = log.filter((l) => l.resultado === 'aceito');
  const exclusoes = {};
  const excluidos = log.filter((l) => l.resultado === 'excluido');
  for (const e of excluidos) exclusoes[e.motivo] = (exclusoes[e.motivo] ?? 0) + 1;
  const arquivos = existsSync(join(pasta, linguagem)) ? readdirSync(join(pasta, linguagem)) : [];
  if (arquivos.length !== aceitos.length) {
    throw new Error(`${linguagem}: ${arquivos.length} arquivos para ${aceitos.length} aceitos`);
  }
  for (const a of aceitos) {
    const arquivo = join(pasta, linguagem, a.solucao.arquivo_local.split(/[\\/]/).pop());
    if (sha(arquivo) !== a.solucao.sha256) throw new Error(`${linguagem}: SHA diferente na ordem ${a.ordem}`);
  }
  return {
    alvo: 20,
    piso: 15,
    percorridos: log.length,
    ultimaOrdem: log.at(-1).ordem,
    aceitos: aceitos.length,
    exclusoesPorMotivo: exclusoes,
    casos: aceitos.map(caso),
    excluidos: excluidos.map((e) => ({ ordem: e.ordem, plataforma: e.plataforma, id: e.id, motivo: e.motivo })),
  };
}

const manifesto = {
  issue: 'R1ck-dev/Code-Insights-plano#23 (IC-16)',
  semente: '20261101',
  criterio: {
    arquivo: 'docs/ifsp-documentacao/criterio-corpus-externo.md (local, fora do git)',
    sha256NaGeracao: sha(criterio),
    sha256DaVersaoCongelada: 'a43d1809559bf30ca877736412421cff6ce9ddad55dddbe110f54cab2ee76fce',
  },
  listas: Object.fromEntries(
    readdirSync(join(pasta, 'listas')).map((f) => [f, sha(join(pasta, 'listas', f))]),
  ),
  sorteio: {
    java: sha(join(pasta, 'sorteio-java.json')),
    c: sha(join(pasta, 'sorteio-c.json')),
  },
  java: porLinguagem('java'),
  c: porLinguagem('c'),
};

writeFileSync(saida, JSON.stringify(manifesto, null, 2) + '\n');
console.log(
  JSON.stringify({ java: manifesto.java.aceitos, c: manifesto.c.aceitos, saida }, null, 2),
);
