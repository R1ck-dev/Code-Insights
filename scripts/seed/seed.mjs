// Semeia a base local com uma coorte ficticia, pela API HTTP.
//
// POR QUE PELA API, e nao por INSERT: cada resolucao precisa passar pelo motor de analise
// estatica de verdade, senao as metricas seriam numeros inventados e as telas de complexidade
// mostrariam algo que o motor nunca produziria. O unico passo que o SQL faz depois e espalhar as
// datas (ver espalhar-datas.sql) — porque a API sempre carimba "agora", e sem isso as seis linhas
// do grafico de evolucao virariam uma coluna so.
//
// Uso:  node scripts/seed/seed.mjs
// Exige: backend no ar em :8080 e a senha do admin (ADMIN_PASSWORD do .env).

import { DESAFIOS } from './desafios.mjs';
import { JAVA } from './solucoes-java.mjs';
import { C } from './solucoes-c.mjs';
import { SNIPPETS } from './snippets.mjs';
import { ALUNOS, PESQUISADORA, SENHA_PADRAO, emailDe, montarTrajetoria } from './personas.mjs';

const API = process.env.API_BASE_URL ?? 'http://localhost:8080';
const ADMIN_EMAIL = process.env.ADMIN_EMAIL ?? 'admin@codeinsights.app';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD;

const CODIGO_POR_LINGUAGEM = { JAVA, C };

let tokenAdmin = null;

function temVersaoEmC(chave, nivel) {
    return Boolean(C[chave]?.[nivel]);
}

function codigoDe(chave, linguagem, nivel) {
    const codigo = CODIGO_POR_LINGUAGEM[linguagem][chave]?.[nivel];
    if (!codigo) {
        throw new Error(`Sem solucao ${linguagem}/${nivel} para "${chave}".`);
    }
    return codigo;
}

async function chamar(metodo, rota, { corpo, token } = {}) {
    const cabecalhos = { 'Content-Type': 'application/json' };
    if (token) {
        cabecalhos.Authorization = `Bearer ${token}`;
    }

    const resposta = await fetch(API + rota, {
        method: metodo,
        headers: cabecalhos,
        body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });

    const texto = await resposta.text();
    if (!resposta.ok) {
        throw new Error(`${metodo} ${rota} -> ${resposta.status} ${texto}`);
    }
    return texto ? JSON.parse(texto) : null;
}

async function entrar(email, senha) {
    const { token } = await chamar('POST', '/api/auth/login', {
        corpo: { email, password: senha },
    });
    return token;
}

/**
 * Registra e ativa. O registro deixa a conta em PENDENTE_VERIFICACAO e dispara um e-mail que
 * ninguem vai abrir; a ativacao administrativa e o mesmo caminho que o admin usa para o aluno que
 * nunca recebeu a mensagem.
 */
async function criarConta(username, email) {
    await chamar('POST', '/api/usuarios', { corpo: { username, email, password: SENHA_PADRAO } });
    await chamar('POST', '/api/admin/usuarios/ativar', { corpo: { email }, token: tokenAdmin });
    return entrar(email, SENHA_PADRAO);
}

async function abrirDesafio(token, desafio) {
    const criado = await chamar('POST', '/api/desafios', {
        token,
        corpo: {
            titulo: desafio.titulo,
            enunciado: desafio.enunciado,
            plataformaOrigem: desafio.plataformaOrigem,
            identificadorExterno: desafio.identificadorExterno,
            urlExterna: desafio.urlExterna,
        },
    });
    await chamar('PATCH', `/api/desafios/${criado.id}/visibilidade`, {
        token,
        corpo: { publico: true },
    });
    return criado.id;
}

async function submeter(token, desafioId, submissao) {
    const resolucao = await chamar('POST', `/api/desafios/${desafioId}/resolucoes`, {
        token,
        corpo: {
            codigoFonte: codigoDe(submissao.desafioChave, submissao.linguagem, submissao.nivel),
            linguagem: submissao.linguagem,
            indiceAutonomiaIA: submissao.autonomia,
            descricaoApoioIA: submissao.descricaoApoioIA,
        },
    });
    await chamar('PATCH', `/api/resolucoes/${resolucao.id}/visibilidade`, {
        token,
        corpo: { publico: true },
    });
    return resolucao.id;
}

async function guardarSnippets(token, indiceDoAluno, desafiosAbertos) {
    // O primeiro da lista e o protagonista das telas de demonstracao: ele leva a biblioteca
    // inteira, para o filtro por categoria da tela de snippets nao aparecer com uma opcao so.
    const quantidade = indiceDoAluno === 0 ? SNIPPETS.length : 2 + (indiceDoAluno % 3);
    const guardados = [];

    for (let i = 0; i < quantidade; i++) {
        const snippet = SNIPPETS[(indiceDoAluno * 3 + i) % SNIPPETS.length];
        // Vincular ao desafio e opcional no dominio, e deixar parte solta mostra os dois estados
        // na tela de snippets.
        const desafioId = i % 2 === 0 ? desafiosAbertos[i % desafiosAbertos.length] : undefined;
        const criado = await chamar('POST', '/api/snippets', {
            token,
            corpo: {
                codigo: snippet.codigo,
                descricao: snippet.descricao,
                categoria: snippet.categoria,
                desafioId,
            },
        });
        guardados.push(criado.id);
    }
    return guardados;
}

async function responderTermo(token, decisao) {
    if (decisao === null) {
        return;
    }
    await chamar('POST', '/api/consentimento', { token, corpo: { autoriza: decisao } });
}

async function semearAluno(aluno, indice) {
    const email = emailDe(aluno.username);
    const token = await criarConta(aluno.username, email);

    await chamar('PATCH', '/api/usuarios/me/visibilidade', {
        token,
        corpo: { publico: aluno.perfilPublico },
    });
    await responderTermo(token, aluno.consentimento);

    const trajetoria = montarTrajetoria(aluno, DESAFIOS, temVersaoEmC);
    const desafioIdPorChave = new Map();

    for (const submissao of trajetoria) {
        if (!desafioIdPorChave.has(submissao.desafioChave)) {
            const desafio = DESAFIOS.find((d) => d.chave === submissao.desafioChave);
            desafioIdPorChave.set(submissao.desafioChave, await abrirDesafio(token, desafio));
        }
        await submeter(token, desafioIdPorChave.get(submissao.desafioChave), submissao);
    }

    const snippets = await guardarSnippets(token, indice, [...desafioIdPorChave.values()]);

    return {
        username: aluno.username,
        desafios: desafioIdPorChave.size,
        resolucoes: trajetoria.length,
        snippets: snippets.length,
    };
}

async function semearPesquisadora() {
    await criarConta(PESQUISADORA.username, PESQUISADORA.email);
    await chamar('POST', '/api/admin/usuarios/promover-pesquisador', {
        corpo: { email: PESQUISADORA.email },
        token: tokenAdmin,
    });
}

async function main() {
    if (!ADMIN_PASSWORD) {
        throw new Error('ADMIN_PASSWORD nao definido. Rode via scripts/semear.ps1, que le o .env.');
    }

    console.log(`Semeando ${API} ...`);
    tokenAdmin = await entrar(ADMIN_EMAIL, ADMIN_PASSWORD);

    await semearPesquisadora();
    console.log(`  pesquisadora ${PESQUISADORA.username} criada e promovida`);

    let totalResolucoes = 0;
    for (const [indice, aluno] of ALUNOS.entries()) {
        const resumo = await semearAluno(aluno, indice);
        totalResolucoes += resumo.resolucoes;
        console.log(
            `  ${resumo.username.padEnd(20)} ${resumo.desafios} desafios, `
            + `${resumo.resolucoes} resolucoes, ${resumo.snippets} snippets`);
    }

    console.log(`\nPronto: ${ALUNOS.length} alunos e ${totalResolucoes} resolucoes.`);
    console.log('A analise das metricas e assincrona — o script de espalhar datas aguarda o fim.');
}

main().catch((erro) => {
    console.error('\nFALHOU:', erro.message);
    process.exit(1);
});
