// Sorteio do corpus externo (IC-16, criterio da IC-15 + adendo 1 de 01/11/2026).
//
// Uso: node scripts/corpus-externo/sorteio.mjs <pasta-das-listas> <pasta-de-saida>
//
// Le as listas publicas guardadas na data de corte, calcula a chave
// SHA-256("20261101:" + plataforma + ":" + id) de cada problema, ordena cada plataforma pela chave
// (crescente) e separa as posicoes: Java fica com as impares (1a, 3a, ...), C com as pares (2a, 4a, ...).
// Escreve um arquivo por linguagem com o rodizio LeetCode/Codeforces ja intercalado. Nada aqui consulta o motor.
import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';

const SEMENTE = '20261101';
const PROFUNDIDADE = 400; // posicoes por plataforma e por linguagem expostas ao coletor
const CATEGORIAS_LEETCODE = new Set(['Algorithms', 'Concurrency']);

const [, , pastaListas, pastaSaida] = process.argv;
if (!pastaListas || !pastaSaida) {
  console.error('uso: node sorteio.mjs <pasta-das-listas> <pasta-de-saida>');
  process.exit(2);
}

const chave = (plataforma, id) =>
  createHash('sha256').update(`${SEMENTE}:${plataforma}:${id}`, 'utf8').digest('hex');

const ler = (nome) => JSON.parse(readFileSync(join(pastaListas, nome), 'utf8'));

function listaCodeforces() {
  const { result } = ler('codeforces-problemset-2026-11-01.json');
  return result.problems.map((p) => ({
    plataforma: 'codeforces',
    id: `${p.contestId}${p.index}`,
    titulo: p.name,
    url: `https://codeforces.com/problemset/problem/${p.contestId}/${p.index}`,
  }));
}

function listaLeetCode() {
  const { questions } = ler('leetcode-questionlist-2026-11-01.json');
  return questions
    .filter((q) => CATEGORIAS_LEETCODE.has(q.categoryTitle))
    .map((q) => ({
      plataforma: 'leetcode',
      id: String(q.questionFrontendId),
      titulo: q.title,
      url: `https://leetcode.com/problems/${q.titleSlug}/`,
      premium: q.paidOnly,
    }));
}

function ordenada(problemas) {
  const ids = new Set();
  for (const p of problemas) {
    if (ids.has(p.id)) throw new Error(`id repetido em ${p.plataforma}: ${p.id}`);
    ids.add(p.id);
  }
  return problemas
    .map((p) => ({ ...p, chave: chave(p.plataforma, p.id) }))
    .sort((a, b) => (a.chave < b.chave ? -1 : a.chave > b.chave ? 1 : 0));
}

function metade(lista, resto) {
  // posicoes de 1 em diante: impares = resto 1, pares = resto 0
  return lista.map((p, i) => ({ ...p, posicao: i + 1 })).filter((p) => p.posicao % 2 === resto);
}

function rodizio(listas) {
  const saida = [];
  for (let i = 0; i < PROFUNDIDADE; i++) {
    for (const lista of listas) if (lista[i]) saida.push({ ordem: saida.length + 1, ...lista[i] });
  }
  return saida;
}

const leetcode = ordenada(listaLeetCode());
const codeforces = ordenada(listaCodeforces());

mkdirSync(pastaSaida, { recursive: true });
const resumo = { semente: SEMENTE, totais: { leetcode: leetcode.length, codeforces: codeforces.length } };
for (const [linguagem, resto] of [['java', 1], ['c', 0]]) {
  const sequencia = rodizio([metade(leetcode, resto), metade(codeforces, resto)]);
  const destino = join(pastaSaida, `sorteio-${linguagem}.json`);
  writeFileSync(destino, JSON.stringify({ linguagem, semente: SEMENTE, sequencia }, null, 1));
  const sha = createHash('sha256').update(readFileSync(destino)).digest('hex');
  resumo[linguagem] = { arquivo: destino, itens: sequencia.length, sha256: sha };
}
console.log(JSON.stringify(resumo, null, 2));
