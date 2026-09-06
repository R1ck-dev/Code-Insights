// Elenco do seed e a curva de evolucao de cada aluno.
//
// A tese da IC e que autonomia sobe e complexidade cai ao longo do tempo. Aqui isso e explicito:
// cada aluno tem um perfil de evolucao, e a trajetoria e derivada dele. Nada e aleatorio — rodar
// o seed duas vezes produz exatamente a mesma coorte, senao as prints de hoje nao bateriam com as
// de amanha.

export const SENHA_PADRAO = 'Senha@2026';

export const MESES_DE_HISTORIA = 6;

export const PESQUISADORA = {
    username: 'pesquisadora.ic',
    email: 'pesquisa@codeinsights.dev',
};

/**
 * `evolucao` decide a curva; `resolucoes` decide o volume.
 * `consentimento`: true aceita, false recusa, null nao respondeu o termo — os tres estados
 * existem porque o relatorio de qualidade da coorte so fica interessante quando ha recusa e
 * pendencia para contrastar.
 */
export const ALUNOS = [
    { username: 'henrique.marangoni', evolucao: 'forte', resolucoes: 14, perfilPublico: true, consentimento: true },
    { username: 'lucas.bertoldo', evolucao: 'forte', resolucoes: 12, perfilPublico: true, consentimento: true },
    { username: 'diego.ferraz', evolucao: 'forte', resolucoes: 11, perfilPublico: true, consentimento: true },
    { username: 'ana.souza', evolucao: 'media', resolucoes: 10, perfilPublico: true, consentimento: true },
    { username: 'mariana.lopes', evolucao: 'media', resolucoes: 9, perfilPublico: true, consentimento: true },
    { username: 'rafael.tavares', evolucao: 'irregular', resolucoes: 9, perfilPublico: true, consentimento: true },
    { username: 'juliana.prado', evolucao: 'forte', resolucoes: 8, perfilPublico: true, consentimento: false },
    { username: 'thiago.nunes', evolucao: 'media', resolucoes: 8, perfilPublico: true, consentimento: true },
    { username: 'camila.rocha', evolucao: 'irregular', resolucoes: 7, perfilPublico: true, consentimento: null },
    { username: 'bruno.antunes', evolucao: 'media', resolucoes: 7, perfilPublico: false, consentimento: true },
    { username: 'leticia.moraes', evolucao: 'forte', resolucoes: 6, perfilPublico: true, consentimento: true },
    { username: 'gustavo.pinheiro', evolucao: 'irregular', resolucoes: 6, perfilPublico: true, consentimento: false },
    { username: 'isabela.freitas', evolucao: 'media', resolucoes: 5, perfilPublico: true, consentimento: null },
];

export function emailDe(username) {
    return username.replace(/\./g, '') + '@codeinsights.dev';
}

/** Fracao do percurso a partir da qual o aluno passa a entregar a versao refinada. */
const LIMIAR_REFINAMENTO = { forte: 0.35, media: 0.55, irregular: 0.6 };

/** Autonomia no inicio e no fim do percurso, por perfil. */
const FAIXA_AUTONOMIA = {
    forte: { inicio: 1, fim: 5 },
    media: { inicio: 2, fim: 4 },
    irregular: { inicio: 2, fim: 4 },
};

const APOIO_POR_AUTONOMIA = {
    1: 'Pedi a solucao completa para a IA e estudei o codigo depois, linha por linha.',
    2: 'A IA escreveu a maior parte. Ajustei os nomes das variaveis e os casos de borda.',
    3: 'Montei a estrutura sozinho e recorri a IA para destravar a parte da complexidade.',
    4: 'Resolvi por conta propria e usei a IA apenas para revisar o codigo no final.',
    5: 'Resolucao inteiramente propria, sem apoio de IA.',
};

function limitar(valor, minimo, maximo) {
    return Math.max(minimo, Math.min(maximo, valor));
}

/**
 * O aluno irregular oscila em vez de subir em linha reta: ele recai de vez em quando, que e o
 * padrao mais comum na vida real e o que impede a coorte inteira de virar uma reta perfeita.
 */
function houveRecaida(evolucao, indice) {
    return evolucao === 'irregular' && indice % 3 === 1;
}

function nivelDaSubmissao(aluno, progresso, indice) {
    if (houveRecaida(aluno.evolucao, indice)) {
        return 'ingenua';
    }
    return progresso >= LIMIAR_REFINAMENTO[aluno.evolucao] ? 'refinada' : 'ingenua';
}

function autonomiaDaSubmissao(aluno, progresso, indice) {
    const faixa = FAIXA_AUTONOMIA[aluno.evolucao];
    const bruta = faixa.inicio + progresso * (faixa.fim - faixa.inicio);
    const recaida = houveRecaida(aluno.evolucao, indice) ? 1 : 0;
    return limitar(Math.round(bruta) - recaida, 1, 5);
}

/**
 * Alterna Java e C, mas so escolhe C quando aquele desafio tem versao em C — o motor le as duas
 * linguagens, e nem todo desafio do catalogo foi escrito nas duas.
 */
function linguagemDaSubmissao(indice, temVersaoEmC) {
    if (indice % 3 === 2 && temVersaoEmC) {
        return 'C';
    }
    return 'JAVA';
}

/**
 * Monta as submissoes de um aluno. Os primeiros `desafiosDistintos` indices abrem desafios novos;
 * os seguintes voltam a um desafio ja aberto, que e como a mesma pessoa reaparece resolvendo o
 * mesmo problema melhor do que antes.
 */
export function montarTrajetoria(aluno, desafios, temVersaoEmC) {
    const total = aluno.resolucoes;
    const desafiosDistintos = Math.min(desafios.length, Math.ceil(total * 0.7));
    const submissoes = [];

    for (let i = 0; i < total; i++) {
        const progresso = total === 1 ? 1 : i / (total - 1);
        const desafio = desafios[i % desafiosDistintos];
        const nivel = nivelDaSubmissao(aluno, progresso, i);
        const autonomia = autonomiaDaSubmissao(aluno, progresso, i);

        submissoes.push({
            desafioChave: desafio.chave,
            nivel,
            linguagem: linguagemDaSubmissao(i, temVersaoEmC(desafio.chave, nivel)),
            autonomia,
            descricaoApoioIA: APOIO_POR_AUTONOMIA[autonomia],
            mes: Math.min(MESES_DE_HISTORIA - 1, Math.floor(progresso * MESES_DE_HISTORIA)),
        });
    }

    return submissoes;
}
