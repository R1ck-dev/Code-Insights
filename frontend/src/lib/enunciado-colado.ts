/*
 * ENUNCIADO COLADO — de um juiz online para o CodeInsights, sem lixo.
 *
 * ── O PROBLEMA (o usuário viu, no Neps Academy) ──────────────────────────────────────────────
 * Copiar "Faça um programa para ler dois valores inteiros A e B…" e colar produz:
 *
 *     Faça um programa para ler dois valores inteiros A A e B B e imprima o resultado da
 *     expressão (A+B) (A+B).
 *
 * Não são "caracteres estranhos": é MATEMÁTICA RENDERIZADA DUAS VEZES. KaTeX (Neps, Codeforces,
 * Beecrowd…) e MathJax emitem, para cada fórmula, DOIS ramos no HTML:
 *   · um MathML invisível (`<span class="katex-mathml">`, `<mjx-assistive-mml>`), que existe para
 *     leitores de tela e carrega o LaTeX de origem numa `<annotation>`; e
 *   · o desenho visual (`<span class="katex-html">`), que é o que enxergamos.
 * O `aria-hidden` esconde um deles do leitor de tela, mas NÃO da área de transferência: o
 * navegador serializa os dois ramos, e cada fórmula sai em dobro. Nenhuma limpeza de "caractere
 * esquisito" resolve isso — o texto está duplicado na origem.
 *
 * ── A SAÍDA ─────────────────────────────────────────────────────────────────────────────────
 * O clipboard não traz só `text/plain`: traz também `text/html`, com a árvore INTEIRA. Aí a
 * duplicata é identificável com precisão — ela tem nome e classe. Este módulo lê o HTML, PODA os
 * ramos invisíveis e extrai o texto do que sobra. É determinístico: não adivinha nada.
 *
 * ⚠ O que NÃO fazemos: reescrever a fórmula. O texto visível do KaTeX já é `(A+B)` — legível, e é
 * exatamente o que o aluno leu no site. Transformar isso em LaTeX exigiria renderizador nas telas
 * de leitura, e o enunciado aqui é texto puro (o campo é um `<textarea>`).
 *
 * Só o `text/plain` (ex.: o usuário passou por um bloco de notas antes) é caso perdido para a
 * poda — ali a duplicata já virou texto e não há como distingui-la com segurança de uma repetição
 * legítima. Nesse caso o campo DETECTA e AVISA, e a remoção é uma escolha explícita do usuário
 * (`removerDuplicacoesAdjacentes`), nunca um silencioso "corrigi para você".
 */

/**
 * Os ramos que existem só para tecnologia assistiva — e que a área de transferência duplica.
 * Podar aqui é o coração da limpeza; o resto deste módulo é higiene de espaços.
 */
const SELETORES_DUPLICATA = [
  'style',
  'script',
  'noscript',
  '.katex-mathml', // KaTeX: MathML + <annotation> com o LaTeX de origem
  'mjx-assistive-mml', // MathJax 3
  '.MJX_Assistive_MathML', // MathJax 2
  '[aria-hidden="true"] math', // MathML solto marcado como decorativo
].join(',')

/** Elementos que, no HTML de origem, significam quebra de parágrafo. */
const BLOCOS = new Set([
  'P', 'DIV', 'SECTION', 'ARTICLE', 'HEADER', 'FOOTER', 'BLOCKQUOTE',
  'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'LI', 'TR', 'PRE', 'UL', 'OL', 'TABLE',
])

/**
 * O texto de um enunciado copiado de uma página, com a matemática contada UMA vez.
 * Devolve `null` quando o HTML não produz texto algum — aí a chamada deve cair no `text/plain`.
 */
export function textoDeHtmlColado(html: string): string | null {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  doc.querySelectorAll(SELETORES_DUPLICATA).forEach((no) => no.remove())

  const texto = extrairTexto(doc.body)
  const limpo = normalizarTexto(texto)
  return limpo || null
}

/** Percorre a árvore acumulando texto e transformando estrutura (blocos, `<br>`) em quebras. */
function extrairTexto(raiz: HTMLElement): string {
  let saida = ''

  const visitar = (no: Node) => {
    if (no.nodeType === Node.TEXT_NODE) {
      saida += no.nodeValue ?? ''
      return
    }
    if (no.nodeType !== Node.ELEMENT_NODE) return

    const elemento = no as Element
    if (elemento.tagName === 'BR') {
      saida += '\n'
      return
    }

    const ehBloco = BLOCOS.has(elemento.tagName)
    if (ehBloco) saida += '\n'
    elemento.childNodes.forEach(visitar)
    if (ehBloco) saida += '\n'
  }

  raiz.childNodes.forEach(visitar)
  return saida
}

/**
 * Higiene do texto — vale para o que veio do HTML e para o `text/plain`.
 *
 * ⚠ Não mexe em acentuação nem em aspas tipográficas: elas são texto legítimo do enunciado. O
 * alvo são os invisíveis que quebram busca e diff (espaço duro, largura zero) e o excesso de
 * espaço que a serialização do HTML sempre produz.
 */
export function normalizarTexto(texto: string): string {
  return texto
    .replace(/\r\n?/g, '\n')
    .replace(/[   ]/g, ' ') // NBSP e parentes: parecem espaço, não são
    .replace(/[​-‍﻿]/g, '') // largura zero: invisíveis de verdade
    .replace(/[ \t]+/g, ' ')
    .replace(/ ?\n ?/g, '\n')
    .replace(/\n{3,}/g, '\n\n') // parágrafo é uma linha em branco, não seis
    .trim()
}

// ════════════════════════════════════════════════════════════════════════════
// FALLBACK: só `text/plain` — aqui a duplicata já virou texto
// ════════════════════════════════════════════════════════════════════════════

/**
 * Um token é "de fórmula" quando repeti-lo lado a lado não faz sentido em português: uma letra
 * solta (`A`, `n`) ou algo com operador/parêntese (`(A+B)`, `2^n`, `A+B`).
 *
 * ⚠ NÚMEROS PUROS ESTÃO FORA, de propósito: enunciados carregam exemplos de entrada ("5 5",
 * "1 1 2"), e ali a repetição é O DADO. Comer isso seria corromper o enunciado para consertar a
 * formatação dele — o oposto do objetivo.
 */
function ehTokenDeFormula(token: string): boolean {
  const nu = token.replace(/[.,;:!?]+$/, '')
  if (!nu || nu.length > 20) return false
  if (/^[A-Za-z]$/.test(nu)) return true
  return /[+\-*/^=<>()]/.test(nu) && /[A-Za-z0-9]/.test(nu)
}

/** Os pares adjacentes idênticos (ignorando pontuação final) que cheiram a fórmula duplicada. */
function paresDuplicados(texto: string): number[] {
  const tokens = texto.split(/(\s+)/) // mantém os separadores: o texto é reconstruível
  const indices: number[] = []

  for (let i = 0; i < tokens.length - 2; i += 2) {
    const atual = tokens[i]
    const proximo = tokens[i + 2]
    if (!atual || !proximo) continue
    // A segunda cópia é a que carrega a pontuação da frase ("(A+B) (A+B)."), então é ela que fica.
    const proximoNu = proximo.replace(/[.,;:!?]+$/, '')
    if (atual === proximoNu && ehTokenDeFormula(atual) && !tokens[i + 1].includes('\n')) {
      indices.push(i)
    }
  }
  return indices
}

/** `true` quando o texto parece ter fórmulas duplicadas — o gatilho do aviso no campo. */
export function temDuplicacaoDeFormula(texto: string): boolean {
  return paresDuplicados(texto).length > 0
}

/**
 * Remove a PRIMEIRA cópia de cada par adjacente idêntico que pareça fórmula. Ação explícita do
 * usuário (um botão), nunca automática: sem o HTML de origem isto é heurística, e heurística que
 * apaga texto sozinha é como se perde a confiança de quem digitou.
 */
export function removerDuplicacoesAdjacentes(texto: string): string {
  const tokens = texto.split(/(\s+)/)
  const remover = new Set(paresDuplicados(texto).flatMap((i) => [i, i + 1]))
  return normalizarTexto(tokens.filter((_, i) => !remover.has(i)).join(''))
}
