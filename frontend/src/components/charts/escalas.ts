/*
 * ESCALAS — a geometria compartilhada dos 3 gráficos (Carta · Linha · Matriz).
 *
 * Toda fórmula aqui foi CONFERIDA contra o markup do protótipo (spec 02 diz onde).
 * Cada bloco cita a origem: "protótipo, spec 02 §X". Onde o protótipo não definia nada,
 * a decisão está marcada como DECISÃO e diz de onde veio (§6-A da entrevista, ou minha).
 *
 * ⚠ A geometria POLAR da Espiral (ex-Órbitas) foi REMOVIDA junto com o gráfico: ele era
 * redundante com a Linha temporal — mesma pergunta (autonomia × tempo, complexidade × tempo),
 * respondida por canais perceptuais mais fracos (tamanho e ângulo, em vez de posição num eixo).
 * `linhasEspectro()` (§3) NÃO caiu: ela é a fonte do card "Distribuição · Espectro" do dashboard.
 *
 * Nada aqui importa React. É matemática pura — testável, e os 3 gráficos consomem igual.
 */
import {
  type ClasseK,
  CLASSES,
  COMPLEXIDADE_ORDEM_MAX,
  TOTAL_CLASSES,
} from '@/domain/enums'
import {
  AUTONOMIA_MAX,
  AUTONOMIA_MIN,
  type ClusterCarta,
  type Constelacao,
  type Granularidade,
  type NivelAutonomia,
  type PontoBase,
  type PontoPlotavel,
  porTempoAsc,
  TOTAL_AUTONOMIA,
} from './tipos'

// ════════════════════════════════════════════════════════════════════════════
// 0 · UTILIDADES
// ════════════════════════════════════════════════════════════════════════════

export interface Ponto2D {
  x: number
  y: number
}

/** Limita `v` ao intervalo [min, max]. */
export function limitar(v: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, v))
}

/** `[{x,y}, …]` → atributo `points` de `<polyline>`/`<polygon>`: "52,204.6 180,242.3". */
export function pontosParaPolyline(pontos: Ponto2D[]): string {
  return pontos.map((p) => `${arred(p.x)},${arred(p.y)}`).join(' ')
}

/** 3 casas: o suficiente para o SVG e sem lixo de ponto flutuante no DOM. */
function arred(v: number): number {
  return Math.round(v * 1000) / 1000
}

/** Número pt-BR com vírgula decimal: `numeroPt(3.75, 1)` → "3,8" (00-INDICE §2.5). */
export function numeroPt(valor: number, casas = 1): string {
  return valor.toFixed(casas).replace('.', ',')
}

/** Data curta `dd.mm` (00-INDICE §2.5 — datas com PONTO, não barra). */
export function dataCurta(d: Date): string {
  return `${doisDigitos(d.getDate())}.${doisDigitos(d.getMonth() + 1)}`
}

/** Data completa `dd.mm.aaaa` — painel "estrela selecionada" ("enviada 14.03.2026"). */
export function dataCompleta(d: Date): string {
  return `${dataCurta(d)}.${d.getFullYear()}`
}

function doisDigitos(n: number): string {
  return String(n).padStart(2, '0')
}

/** Meses pt-BR, 3 letras minúsculas — rótulos do eixo X da Linha (protótipo, spec 02 §5.3). */
export const MESES_CURTOS = [
  'jan', 'fev', 'mar', 'abr', 'mai', 'jun',
  'jul', 'ago', 'set', 'out', 'nov', 'dez',
] as const

// ════════════════════════════════════════════════════════════════════════════
// 1 · CARTA (scatter) — autonomia × complexidade
// ════════════════════════════════════════════════════════════════════════════

/** `viewBox="0 0 580 322"` — razão 1.80 (protótipo, spec 02 §2). */
export const CARTA_VIEWBOX = { largura: 580, altura: 322 } as const

/** Área de plotagem: dados em x ∈ [52, 556], y ∈ [16, 280]; a GRADE vai até x=564. */
export const CARTA_PLOT = { x0: 52, x1: 556, y0: 16, y1: 280 } as const
export const CARTA_GRADE_X_FIM = 564

/**
 * x = [52, 180, 308, 436, 556][a-1] — protótipo, spec 02 §2.2 (tabela LITERAL).
 * A progressão regular (`52 + (a-1)*128`) daria 564 na coluna 5, mas o protótipo recua para
 * 556 para manter o halo da estrela dentro da grade. São 5 valores: use a tabela.
 */
export const CARTA_X: readonly number[] = [52, 180, 308, 436, 556]

export function xDaAutonomia(a: number): number {
  const nivel = limitar(Math.round(a), AUTONOMIA_MIN, AUTONOMIA_MAX)
  return CARTA_X[nivel - 1]
}

/** Altura de uma faixa de classe: 264 / 7 = 37.714… (protótipo, spec 02 §2.3). */
export const CARTA_PASSO_Y = (CARTA_PLOT.y1 - CARTA_PLOT.y0) / COMPLEXIDADE_ORDEM_MAX

/**
 * y = 280 − k · (264/7) — protótipo, spec 02 §2.3.
 * O(1) (k=0) EMBAIXO (y=280), O(n!) (k=7) NO TOPO (y=16). Conferido: os 12 pontos, a
 * cruzeta e as 8 linhas de grade do protótipo caem exatamente nestes y.
 */
export function yDaClasse(k: number): number {
  const classe = limitar(k, 0, COMPLEXIDADE_ORDEM_MAX)
  return CARTA_PLOT.y1 - classe * CARTA_PASSO_Y
}

/** Posição exata de um ponto na Carta. É TAMBÉM a posição do cluster dele — não há jitter. */
export function posicaoNaCarta(p: PontoPlotavel): Ponto2D {
  return { x: xDaAutonomia(p.autonomia), y: yDaClasse(p.k) }
}

// ── CLUSTERS (substituem o jitter) ──────────────────────────────────────────
// PROBLEMA (visto no app): a Carta é discreta (5 × 8 = 40 células). Várias resoluções na mesma
// célula eram espalhadas num anel de 5px pelo antigo `deslocamentosCoincidentes` — marcadores
// grudados, alvo de clique impossível e DOIS callouts abrindo ao mesmo tempo.
//
// DECISÃO: o jitter morreu. Cada célula ocupada vira UM marcador na posição EXATA, que cresce
// com a quantidade e escreve o número no núcleo quando `total >= 2`. O clique abre a resolução
// mais ANTIGA do grupo, e o painel navega entre as irmãs ("‹ 2 de 3 ›").

/** A identidade da célula: autonomia × classe. */
export function chaveDoCluster(p: { autonomia: number; k: number }): string {
  return `${p.autonomia}:${p.k}`
}

export const CLUSTER_NUCLEO_BASE = 2.6
export const CLUSTER_HALO_BASE = 7
/** Acima disto o marcador PARA de crescer (a curva satura). */
export const CLUSTER_SATURACAO = 8
/** Ganho máximo de raio do núcleo: 2.6 (n=1) → 5.0 (n ≥ 8). */
export const CLUSTER_NUCLEO_GANHO = 2.4
/** A partir de 2 resoluções o número aparece no núcleo. */
export const CLUSTER_ROTULO_MIN = 2

/**
 * Raio do marcador de um cluster. `total = 1` devolve EXATAMENTE a estrela de hoje
 * (halo 7 · núcleo 2.6) — clusters não podem mudar a aparência de quem está sozinho.
 *
 * DECISÃO (curva): **logarítmica saturada** —
 *   núcleo(total) = 2.6 + 2.4 · ln(total) / ln(8), limitado a [2.6, 5.0]
 *
 * Por que log e não linear: a informação está no PRIMEIRO salto. "1 → 2" significa que o aluno
 * VOLTOU ao mesmo ponto (mesma autonomia, mesma classe) — o dado interessante — e ganha +0,8px,
 * bem visível. "7 → 8" é ruído e ganha ~0,15px. Linear (`+0.7` por unidade) faria um cluster de
 * 8 virar um balão de 7,5px que invadiria a célula vizinha; a raiz quadrada cresceria demais no
 * meio da escala. O halo acompanha na mesma razão do original (7 / 2.6 ≈ 2,69), então a silhueta
 * da estrela é a mesma em qualquer tamanho. Acima de 8 o marcador congela: quem quer o número
 * exato lê o rótulo do núcleo, que não mente nem satura.
 */
export function raioDoCluster(total: number): { halo: number; nucleo: number } {
  const n = Math.max(1, Math.floor(total))
  const fator = limitar(Math.log(n) / Math.log(CLUSTER_SATURACAO), 0, 1)
  const nucleo = CLUSTER_NUCLEO_BASE + CLUSTER_NUCLEO_GANHO * fator
  return {
    halo: arred(nucleo * (CLUSTER_HALO_BASE / CLUSTER_NUCLEO_BASE)),
    nucleo: arred(nucleo),
  }
}

/**
 * Os pontos plotáveis agrupados por CÉLULA. A ordem dos clusters segue a do primeiro ponto de
 * cada grupo (= cronológica, porque `montarDataset` entrega `pontos` ordenado); dentro do
 * cluster, `pontos` está em ordem cronológica ASC — `pontos[0]` é a mais antiga.
 */
export function agruparEmClusters(pontos: PontoPlotavel[]): ClusterCarta[] {
  const grupos = new Map<string, PontoPlotavel[]>()
  for (const p of pontos) {
    const chave = chaveDoCluster(p)
    const g = grupos.get(chave)
    if (g) g.push(p)
    else grupos.set(chave, [p])
  }

  const clusters: ClusterCarta[] = []
  for (const [chave, doGrupo] of grupos) {
    const ordenados = [...doGrupo].sort(porTempoAsc)
    const primeiro = ordenados[0]
    clusters.push({
      chave,
      autonomia: primeiro.autonomia,
      k: primeiro.k,
      x: xDaAutonomia(primeiro.autonomia),
      y: yDaClasse(primeiro.k),
      pontos: ordenados,
      total: ordenados.length,
    })
  }
  return clusters
}

/** O cluster que contém uma resolução — para destacar o marcador selecionado. */
export function clusterDoPonto(
  clusters: ClusterCarta[],
  resolucaoId: string | null | undefined,
): ClusterCarta | null {
  if (!resolucaoId) return null
  return clusters.find((c) => c.pontos.some((p) => p.resolucaoId === resolucaoId)) ?? null
}

/**
 * As resoluções que dividem a célula com `resolucaoId` — INCLUSIVE ela —, em ordem cronológica.
 * O painel lateral usa isto nas setas "‹ 2 de 3 ›". Resolução sozinha devolve `[ela]` (array de
 * 1 → o painel esconde as setas). Id inexistente devolve `[]`.
 */
export function irmaosDoCluster(pontos: PontoPlotavel[], resolucaoId: string): PontoPlotavel[] {
  const alvo = pontos.find((p) => p.resolucaoId === resolucaoId)
  if (!alvo) return []
  const chave = chaveDoCluster(alvo)
  return pontos.filter((p) => chaveDoCluster(p) === chave).sort(porTempoAsc)
}

/** Posição (base 0) da resolução dentro do seu cluster; `-1` se ela não está na lista. */
export function indiceNoCluster(pontos: PontoPlotavel[], resolucaoId: string): number {
  return irmaosDoCluster(pontos, resolucaoId).findIndex((p) => p.resolucaoId === resolucaoId)
}

// ── CONSTELAÇÕES sobre clusters ─────────────────────────────────────────────

export interface TracoConstelacao {
  desafioId: string
  desafioTitulo: string
  /** Posições em sequência cronológica, SEM segmentos degenerados. `length >= 2`. */
  posicoes: Ponto2D[]
  /** Pronto para `<polyline points={...}>`. */
  polyline: string
}

/**
 * As posições de uma constelação, com os segmentos DEGENERADOS removidos.
 *
 * Por que: a constelação liga as resoluções do mesmo desafio em ordem cronológica. Com clusters,
 * duas resoluções seguidas do mesmo desafio podem cair na MESMA célula (mesma autonomia, mesma
 * classe — o aluno tentou de novo e chegou ao mesmo lugar) e o segmento teria comprimento zero:
 * uma `polyline` com pontos repetidos gera artefato de `stroke-linejoin` e um "nó" que parece
 * sujeira. Colapsamos só as repetições CONSECUTIVAS — A→B→A continua sendo um V legítimo, e não
 * pode ser achatado.
 */
export function posicoesDaConstelacao(constelacao: Constelacao): Ponto2D[] {
  const posicoes: Ponto2D[] = []
  for (const p of constelacao.pontos) {
    const atual = posicaoNaCarta(p)
    const anterior = posicoes[posicoes.length - 1]
    if (anterior && anterior.x === atual.x && anterior.y === atual.y) continue
    posicoes.push(atual)
  }
  return posicoes
}

/**
 * Os traços desenháveis. Uma constelação cujas resoluções caem TODAS na mesma célula sobra com 1
 * posição e não vira traço — mas continua existindo no `dataset` (o cluster já a declara pelo
 * número no núcleo). Descartar o traço não descarta o dado.
 */
export function tracosDasConstelacoes(constelacoes: Constelacao[]): TracoConstelacao[] {
  const tracos: TracoConstelacao[] = []
  for (const c of constelacoes) {
    const posicoes = posicoesDaConstelacao(c)
    if (posicoes.length < 2) continue
    tracos.push({
      desafioId: c.desafioId,
      desafioTitulo: c.desafioTitulo,
      posicoes,
      polyline: pontosParaPolyline(posicoes),
    })
  }
  return tracos
}

// ── Eixo Y = a própria barra de colormap (spec 02 §2.4) ─────────────────────

export const CARTA_EIXO_Y = { x: 40, largura: 7 } as const

export interface CelulaEixoY {
  k: ClasseK
  y: number
  altura: number
}

/**
 * As 7 células da barra do eixo Y (k = 1..7). Cada célula é a faixa ABAIXO da linha da sua
 * classe: `[y(k), y(k) + 37.71]` (protótipo, spec 02 §2.4 — confere com os rects do markup).
 */
export function celulasEixoY(): CelulaEixoY[] {
  const celulas: CelulaEixoY[] = []
  for (let k = 1; k <= COMPLEXIDADE_ORDEM_MAX; k++) {
    celulas.push({ k: k as ClasseK, y: arred(yDaClasse(k)), altura: arred(CARTA_PASSO_Y) })
  }
  return celulas
}

/**
 * DECISÃO (spec 02, Lacuna C — proposta da própria spec, adotada): a barra tem 7 células
 * para 8 classes; a faixa de O(1) cairia ABAIXO da linha de base. Sem isso o verde nunca
 * aparece no eixo. Quadrado 7×7 centrado na linha de base (y = 280 − 3.5 = 276.5) = o "piso"
 * da escala.
 */
export const CARTA_CELULA_O1 = { x: 40, y: 276.5, lado: 7, k: 0 as ClasseK } as const

/**
 * DECISÃO (spec 02, Lacuna D — proposta da spec, adotada): ticks do eixo Y em k = 0, 2, 4, 7
 * (`O(1)`, `O(n)`, `n²`, `n!`) — 4 rótulos, sem poluir. Mono 9px, `text-anchor="end"`, x=36
 * (à esquerda da barra, que começa em x=40). Cabe no viewBox: NÃO mexo em 580×322.
 */
export const CARTA_TICKS_Y: readonly ClasseK[] = [0, 2, 4, 7]
export const CARTA_TICK_Y_X = 36

/** Ticks do eixo X: 1..5 em y=298; título "AUTONOMIA IA →" em x=308, y=316 (spec 02 §2.10). */
export const CARTA_TICK_X_Y = 298
export const CARTA_TITULO_X = { x: 308, y: 316 } as const

// ── Callout da Carta ────────────────────────────────────────────────────────

export interface PosicaoCallout {
  left: string
  top: string
  transform: string
}

/**
 * O callout é um `<div>` absoluto SOBRE o SVG: `left = x/largura`, `top = y/altura` (spec
 * 02 §2.9 — o protótipo escreveu `top:33%` a olho; a fórmula é a verdade).
 *
 * Lacuna F (colisão com a borda), proposta da spec adotada: perto da direita o callout
 * inverte para a esquerda; perto do topo, cresce para baixo. Limiares em fração do viewBox
 * (x > 69% ≈ o `x>400` que a spec cita para 580; y < 18.6% ≈ o `y<60` dela).
 */
export function posicaoCallout(
  x: number,
  y: number,
  viewBox: { largura: number; altura: number } = CARTA_VIEWBOX,
): PosicaoCallout {
  const inverterX = x > viewBox.largura * 0.69
  const inverterY = y < viewBox.altura * 0.186
  const tx = inverterX ? 'calc(-100% - 10px)' : '10px'
  const ty = inverterY ? '0' : '-100%'
  return {
    left: `${arred((x / viewBox.largura) * 100)}%`,
    top: `${arred((y / viewBox.altura) * 100)}%`,
    transform: `translate(${tx}, ${ty})`,
  }
}

// ════════════════════════════════════════════════════════════════════════════
// 3 · ESPECTRO (histograma por classe) — HTML/CSS, não SVG
// ════════════════════════════════════════════════════════════════════════════

export interface LinhaEspectro {
  k: ClasseK
  /** rótulo curto do eixo: 'O(1)', 'log n', 'O(n)', 'n log n', 'n²', 'n³', '2ⁿ', 'n!'. */
  curto: string
  contagem: number
  /** largura da barra em % (0..100) — já pronta para `style={{ width: `${largura}%` }}`. */
  largura: number
}

/** Contagem por classe: array de 8 posições, índice = k. Sempre 8 (0 é contagem legítima). */
export function contagemPorClasse(pontos: PontoPlotavel[]): number[] {
  const contagens = new Array<number>(TOTAL_CLASSES).fill(0)
  for (const p of pontos) contagens[p.k]++
  return contagens
}

/** largura% = contagem / max(contagem) · 100 — protótipo, spec 02 §4.1 (conferido nas 6 linhas). */
export function larguraEspectro(contagem: number, maxContagem: number): number {
  if (maxContagem <= 0) return 0
  return arred((contagem / maxContagem) * 100)
}

/**
 * As 8 linhas do espectro, SEMPRE (recomendação do índice §6, item 8: eixo estável — o
 * "espectro completo" é a metáfora; classe com 0 fica com o trilho vazio). O tile compacto
 * do dashboard, se quiser só as 5 maiores, faz o slice — a base é completa.
 * A ordem é k=0..7 (do mais eficiente ao mais custoso).
 */
export function linhasEspectro(pontos: PontoPlotavel[]): LinhaEspectro[] {
  const contagens = contagemPorClasse(pontos)
  const max = Math.max(0, ...contagens)
  return CLASSES.map((c) => ({
    k: c.k,
    curto: c.curto,
    contagem: contagens[c.k],
    largura: larguraEspectro(contagens[c.k], max),
  }))
}

// ════════════════════════════════════════════════════════════════════════════
// 4 · LINHA (evolução temporal) — buckets DIÁRIOS / SEMANAIS / MENSAIS
// ════════════════════════════════════════════════════════════════════════════

/** `viewBox="0 0 400 220"` — protótipo, spec 02 §5. */
export const LINHA_VIEWBOX = { largura: 400, altura: 220 } as const

/** Dados em x ∈ [40, 384] (largura 344); banda vertical y ∈ [60, 200] (protótipo, §5.2). */
export const LINHA_X0 = 40
export const LINHA_LARGURA = 344
export const LINHA_Y_TOPO = 60
export const LINHA_Y_BASE = 200

/**
 * Quantos buckets cabem na janela, por granularidade — a janela é sempre "os N mais RECENTES".
 *
 * DECISÃO (limite é PIXEL, não gosto): a faixa de dados tem 344px. O rótulo mono 9px mais largo
 * é `mmm/aa` (~34px) e o `dd.mm` (~28px).
 *   · MENSAL  = 8  → 49,1px por bucket. 8 meses ≈ 2 semestres: a escala em que um portfólio se
 *                    move. É a janela do protótipo, e a padrão.
 *   · SEMANAL = 12 → 31,3px por bucket: um trimestre, com todos os `dd.mm` ainda cabendo.
 *   · DIARIO  = 14 → 26,5px por bucket: duas semanas. Aqui os rótulos JÁ NÃO CABEM todos —
 *                    use `passoDeRotulos(n)` e desenhe um sim, um não. O eixo continua com 14
 *                    posições (o tempo não encolhe); só a rotulagem é rala.
 */
export const MAX_BUCKETS_POR_GRANULARIDADE: Record<Granularidade, number> = {
  DIARIO: 14,
  SEMANAL: 12,
  MENSAL: 8,
}

/** Com menos de 2 buckets COM DADO não há série — a tela mostra o estado vazio (Lacuna M). */
export const LINHA_MIN_PONTOS = 2

/**
 * Largura estimada do rótulo do eixo X, em mono 9px: `dd.mm` (5 glifos) ≈ 28px · `mmm/aa`
 * (6 glifos) ≈ 34px. É o que decide se os rótulos cabem lado a lado.
 */
const LINHA_ROTULO_LARGURA: Record<Granularidade, number> = {
  DIARIO: 28,
  SEMANAL: 28,
  MENSAL: 34,
}

/**
 * De quantos em quantos buckets rotular o eixo X, para os rótulos não se sobreporem.
 * Devolve 1 (todos), 2 (um sim, um não)… — o componente rotula quando `i % passo === 0`, e deve
 * SEMPRE rotular também o ÚLTIMO bucket (é a data de referência da leitura).
 *
 * Com as janelas padrão: MENSAL(8) → 1 · SEMANAL(12) → 1 · DIARIO(14) → 2 (26,5px por bucket não
 * comportam um `dd.mm` de 28px em cada). ⚠ Ralear RÓTULO não é ralear BUCKET: os 14 dias
 * continuam ocupando o eixo, incluindo os vazios.
 */
export function passoDeRotulos(n: number, granularidade: Granularidade): number {
  if (n <= 1) return 1
  const espacamento = LINHA_LARGURA / (n - 1)
  return Math.max(1, Math.ceil(LINHA_ROTULO_LARGURA[granularidade] / espacamento))
}

/**
 * x(i) = 40 + i · (344 / (n−1)) — protótipo, spec 02 §5.2.
 * Com n=8 dá 40, 89.1, 138.3, …, 384 — bate com os 8 x do markup. Generalizado para n
 * buckets (o protótipo fixava 344/7 porque tinha exatamente 8). n=1 → centro da área.
 */
export function xDaLinha(i: number, n: number): number {
  if (n <= 1) return LINHA_X0 + LINHA_LARGURA / 2
  return arred(LINHA_X0 + i * (LINHA_LARGURA / (n - 1)))
}

/** y_autonomia(a) = 200 − (a−1)·35 — a ∈ [1,5] (REAL: é média) → 200…60. Protótipo, §5.2. */
export function yAutonomiaLinha(a: number): number {
  const v = limitar(a, AUTONOMIA_MIN, AUTONOMIA_MAX)
  return arred(LINHA_Y_BASE - (v - AUTONOMIA_MIN) * 35)
}

/**
 * y_complexidade(k) = 200 − k·20 — k ∈ [0,7] (REAL: é média de classes) → 200…60.
 * Protótipo, §5.2. Mesma banda de pixels da autonomia, unidades diferentes (eixo duplo
 * normalizado). A ORIENTAÇÃO É A DA CARTA: mais alto = classe mais custosa (Lacuna L).
 */
export function yClasseLinha(k: number): number {
  const v = limitar(k, 0, COMPLEXIDADE_ORDEM_MAX)
  return arred(LINHA_Y_BASE - v * 20)
}

/**
 * DECISÃO (spec 02, Lacuna K — proposta da spec, adotada): a grade do protótipo (y=60/110/160)
 * não cai em nenhum valor inteiro. Regrar nos INTEIROS DE AUTONOMIA: y = 60, 95, 130, 165, 200
 * = 5, 4, 3, 2, 1. Assim existe eixo Y legível (hoje inexistente).
 */
export const LINHA_GRADE_Y: readonly { autonomia: NivelAutonomia; y: number }[] = [
  { autonomia: 5, y: 60 },
  { autonomia: 4, y: 95 },
  { autonomia: 3, y: 130 },
  { autonomia: 2, y: 165 },
  { autonomia: 1, y: 200 },
]

/** Ticks de classe à direita (opcionais): k = 0, 2, 4, 7 → y = 200, 160, 120, 60. */
export const LINHA_TICKS_CLASSE: readonly ClasseK[] = [0, 2, 4, 7]

export interface BucketTempo {
  granularidade: Granularidade
  /** `aaaa-mm-dd` (dia / semana) ou `aaaa-mm` (mês) — chave estável de React. */
  chave: string
  /** Rótulo do eixo X: `dd.mm` (dia e semana) · `mmm/aa` (mês). */
  rotulo: string
  /** Rótulo por extenso, para o callout: `14.03.2026` · `semana de 09.03.2026` · `mar/2026`. */
  rotuloLongo: string
  /** Instante inicial do bucket (meia-noite local do dia / da segunda-feira / do 1º do mês). */
  inicio: Date
  /** TODAS as resoluções do bucket — inclusive as sem métrica (autonomia independe da linguagem). */
  resolucoes: PontoBase[]
  /** Só as com classe de tempo: a série de COMPLEXIDADE sai daqui. */
  comMetrica: PontoBase[]
  /** = `resolucoes.length`. Zero = bucket SEM RESOLUÇÃO (o aluno não enviou nada). */
  total: number
  /** Quantas do bucket não têm classe de complexidade. `total > 0 && semMetrica === total` é um
   *  período de trabalho SEM métrica — jamais um período "vazio". */
  semMetrica: number
  /** média de autonomia sobre TODAS as resoluções; `null` só no bucket sem resolução. */
  mediaAutonomia: number | null
  /** média de `k` (classe de tempo) sobre as COM métrica; `null` quando nenhuma tem classe. */
  mediaClasse: number | null
}

function media(valores: number[]): number | null {
  if (valores.length === 0) return null
  return valores.reduce((s, v) => s + v, 0) / valores.length
}

// ── Aritmética de calendário ────────────────────────────────────────────────
// Todo bucket vira um ÍNDICE INTEIRO em que buckets consecutivos são inteiros consecutivos —
// é isso que faz o `for (idx = inicio; idx <= max; idx++)` produzir a janela CONTÍNUA (com os
// buracos ocupando o seu x). Os dias são contados via `Date.UTC(ano, mês, dia)` a partir das
// partes LOCAIS: dá um número de dia estável, imune a horário de verão (uma subtração de
// timestamps locais erraria por 1h duas vezes por ano, e um dia inteiro na virada).

const MS_POR_DIA = 86_400_000
/** 1970-01-01 foi quinta; a primeira segunda (1970-01-05) é o dia absoluto 4. */
const SEGUNDA_EPOCH = 4

function diaAbsoluto(d: Date): number {
  return Math.floor(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()) / MS_POR_DIA)
}

/** Dia absoluto → `Date` na meia-noite LOCAL daquele dia (inverso exato de `diaAbsoluto`). */
function dataDoDiaAbsoluto(dia: number): Date {
  const utc = new Date(dia * MS_POR_DIA)
  return new Date(utc.getUTCFullYear(), utc.getUTCMonth(), utc.getUTCDate())
}

/** Dia absoluto da SEGUNDA-FEIRA da semana de `d` (semana ISO: segunda → domingo). */
function segundaDaSemana(d: Date): number {
  const dia = diaAbsoluto(d)
  const desdeSegunda = (d.getDay() + 6) % 7 // getDay: 0=dom … 6=sáb
  return dia - desdeSegunda
}

function indiceDoBucket(d: Date, g: Granularidade): number {
  if (g === 'MENSAL') return d.getFullYear() * 12 + d.getMonth()
  if (g === 'SEMANAL') return (segundaDaSemana(d) - SEGUNDA_EPOCH) / 7
  return diaAbsoluto(d)
}

function inicioDoBucket(idx: number, g: Granularidade): Date {
  if (g === 'MENSAL') return new Date(Math.floor(idx / 12), idx % 12, 1)
  if (g === 'SEMANAL') return dataDoDiaAbsoluto(idx * 7 + SEGUNDA_EPOCH)
  return dataDoDiaAbsoluto(idx)
}

function chaveDoBucket(inicio: Date, g: Granularidade): string {
  const ano = inicio.getFullYear()
  const mes = doisDigitos(inicio.getMonth() + 1)
  if (g === 'MENSAL') return `${ano}-${mes}`
  return `${ano}-${mes}-${doisDigitos(inicio.getDate())}`
}

function rotulosDoBucket(inicio: Date, g: Granularidade): { rotulo: string; longo: string } {
  const mes = MESES_CURTOS[inicio.getMonth()]
  const ano = inicio.getFullYear()
  if (g === 'MENSAL') {
    return { rotulo: `${mes}/${String(ano).slice(-2)}`, longo: `${mes}/${ano}` }
  }
  if (g === 'SEMANAL') {
    return { rotulo: dataCurta(inicio), longo: `semana de ${dataCompleta(inicio)}` }
  }
  return { rotulo: dataCurta(inicio), longo: dataCompleta(inicio) }
}

/**
 * Os buckets do eixo do tempo, na granularidade pedida (spec 02 §5 + Lacuna M).
 *
 * ⚠ Recebe `PontoBase[]` = `dataset.todas`, NÃO `dataset.pontos`. Um mês em que o aluno submeteu
 * 3 resoluções em Python TEM 3 resoluções — ele não é um mês "sem resolução". A autonomia é
 * autodeclarada e independe da linguagem (§4.4): a série neutra atravessa o período; só a série
 * de complexidade se interrompe. É por isso que `mediaAutonomia` sai de `resolucoes` e
 * `mediaClasse` sai de `comMetrica`.
 *
 * ⚠ Tudo aqui é CLIENT-SIDE, derivado do mesmo `dataset` dos outros 4 gráficos. Não existe
 * chamada a `/api/metricas/evolucao`: duas fontes de verdade para o mesmo número divergem, e
 * essa divergência é impossível de explicar ao aluno (§4.5).
 *
 * INVARIANTES (não regredir — são o que torna o gráfico honesto):
 *   1. A janela é o intervalo CONTÍNUO do primeiro ao último bucket COM ATIVIDADE. Bucket vazio
 *      NÃO some: ocupa o seu `x`. O tempo não anda mais devagar porque o aluno parou de enviar.
 *   2. Se o intervalo passar de `maxBuckets`, ficam os N MAIS RECENTES.
 *   3. Bucket sem resolução → `mediaAutonomia = null` → NÃO vira ponto. A linha ATRAVESSA o vão
 *      com traço TRACEJADO (`trechosDaSerie`), que declara "aqui não houve medição": nenhum
 *      marcador no vão, nenhum valor intermediário inventado. Interpolar (valor no meio) é
 *      mentir; não ligar nada era o bug que emudecia o diário/semanal.
 *      Bucket com resolução mas sem métrica → só `mediaClasse` é `null`: a linha de complexidade
 *      atravessa tracejada, a de autonomia segue sólida (autonomia independe da linguagem).
 *
 * Recebe as resoluções em ordem cronológica ascendente (garantia de `montarDataset`).
 */
export function buckets(
  resolucoes: PontoBase[],
  granularidade: Granularidade,
  maxBuckets = MAX_BUCKETS_POR_GRANULARIDADE[granularidade],
): BucketTempo[] {
  if (resolucoes.length === 0) return []

  const porIndice = new Map<number, PontoBase[]>()
  let min = Number.POSITIVE_INFINITY
  let max = Number.NEGATIVE_INFINITY

  for (const p of resolucoes) {
    const idx = indiceDoBucket(p.submetidaEm, granularidade)
    min = Math.min(min, idx)
    max = Math.max(max, idx)
    const g = porIndice.get(idx)
    if (g) g.push(p)
    else porIndice.set(idx, [p])
  }

  const primeiro = Math.max(min, max - Math.max(1, maxBuckets) + 1)
  const saida: BucketTempo[] = []
  for (let idx = primeiro; idx <= max; idx++) {
    const doBucket = porIndice.get(idx) ?? []
    const comMetrica = doBucket.filter((p) => p.k != null)
    const inicio = inicioDoBucket(idx, granularidade)
    const { rotulo, longo } = rotulosDoBucket(inicio, granularidade)
    saida.push({
      granularidade,
      chave: chaveDoBucket(inicio, granularidade),
      rotulo,
      rotuloLongo: longo,
      inicio,
      resolucoes: doBucket,
      comMetrica,
      total: doBucket.length,
      semMetrica: doBucket.length - comMetrica.length,
      mediaAutonomia: media(doBucket.map((p) => p.autonomia)),
      mediaClasse: media(comMetrica.map((p) => p.k as number)),
    })
  }
  return saida
}

/** Qual das duas séries do gráfico de Linha. */
export type SerieLinha = 'autonomia' | 'classe'

export interface PontoSerie {
  /** índice do bucket (define o x). */
  i: number
  x: number
  y: number
  /** o valor bruto da média (autonomia 1..5 ou classe 0..7) — para o tooltip. */
  valor: number
  bucket: BucketTempo
}

/**
 * Os pontos de uma série (só os buckets COM dado). Buckets vazios não viram ponto.
 * `janela` = a saída de `buckets(...)` (o parâmetro não se chama `buckets` para não sombrear a
 * função de mesmo nome).
 */
export function pontosDaSerie(janela: BucketTempo[], serie: SerieLinha): PontoSerie[] {
  const n = janela.length
  const saida: PontoSerie[] = []
  janela.forEach((bucket, i) => {
    const valor = serie === 'autonomia' ? bucket.mediaAutonomia : bucket.mediaClasse
    if (valor == null) return // sem resolução: sem ponto (e a linha quebra aqui)
    saida.push({
      i,
      x: xDaLinha(i, n),
      y: serie === 'autonomia' ? yAutonomiaLinha(valor) : yClasseLinha(valor),
      valor,
      bucket,
    })
  })
  return saida
}

/**
 * Um TRECHO desenhável da série: uma `<polyline>`.
 *
 * `continuo` é a única coisa que o gráfico precisa saber para escolher o traço:
 *   • `true`  → os buckets são VIZINHOS e todos têm dado: medição contígua → traço SÓLIDO.
 *   • `false` → é uma PONTE sobre um ou mais buckets sem dado → traço TRACEJADO.
 */
export interface TrechoSerie {
  /** Chave estável de React. */
  chave: string
  /** ≥ 2 pontos, em ordem de bucket. Numa ponte são exatamente 2: as bordas do vão. */
  pontos: PontoSerie[]
  /** `false` = o trecho ATRAVESSA buckets sem dado (traço tracejado, sem marcador no meio). */
  continuo: boolean
}

/**
 * A série fatiada em trechos SÓLIDOS (buckets vizinhos, ambos com dado) e PONTES TRACEJADAS
 * (dois buckets com dado separados por um ou mais buckets sem dado).
 *
 * ── POR QUE ISTO EXISTE (bug real) ─────────────────────────────────────────────────────
 * A versão anterior (`segmentosDaSerie`) quebrava a série em todo bucket sem dado e parava por
 * aí — "não interpolar", regra certa. Só que no MENSAL os meses costumam ser contíguos (havia
 * segmentos de 2+ pontos, e a linha se formava), enquanto no SEMANAL e no DIÁRIO quase todo
 * bucket entre duas resoluções está vazio: cada ponto virava um segmento de tamanho 1, e uma
 * `polyline` de um ponto só NÃO DESENHA NADA. O gráfico ficava mudo — pontos soltos, sem linha.
 *
 * ── A HONESTIDADE, PRESERVADA E EXPLÍCITA ──────────────────────────────────────────────
 * A ponte liga os dois buckets com dado ATRAVESSANDO o vão, mas o traço é TRACEJADO, e isso
 * significa exatamente: "aqui não houve medição; a linha é continuidade visual, não dado".
 *   · NENHUM marcador é desenhado em bucket sem dado (o marcador continua saindo de
 *     `pontosDaSerie`, que só conhece bucket COM dado): nada de valor intermediário inventado.
 *   · O bucket vazio CONTINUA ocupando o seu `x` no eixo (`buckets()`, invariante 1): o tempo
 *     não anda mais devagar porque o aluno parou de enviar.
 *   · O gráfico declara o código na legenda ("tracejado = período sem medição").
 * Interpolar seria afirmar uma medição que não existe; NÃO ligar era esconder a série inteira.
 * O tracejado diz as duas coisas: houve continuidade no tempo, não houve medição no meio.
 *
 * Vale para as DUAS séries e nas TRÊS granularidades — inclusive no mensal, que também pode ter
 * um mês vazio no meio. ⚠ "Sem dado" quer dizer coisas diferentes em cada série, de propósito:
 * na autonomia é bucket SEM RESOLUÇÃO; na complexidade é bucket sem nenhuma resolução
 * CLASSIFICADA (um mês só de Python tem autonomia e não tem complexidade — §4.4).
 */
export function trechosDaSerie(janela: BucketTempo[], serie: SerieLinha): TrechoSerie[] {
  const pontos = pontosDaSerie(janela, serie)
  const trechos: TrechoSerie[] = []
  let corrida: PontoSerie[] = []

  // Corrida de 1 ponto não vira trecho: uma polyline de um ponto só não desenha nada, e o
  // marcador daquele bucket já é desenhado pelo componente. Ele não fica órfão — as pontes
  // tracejadas o ligam aos vizinhos com dado.
  const fecharCorrida = () => {
    if (corrida.length >= 2) {
      trechos.push({ chave: `s-${corrida[0].bucket.chave}`, pontos: corrida, continuo: true })
    }
    corrida = []
  }

  for (const p of pontos) {
    const anterior = corrida[corrida.length - 1]
    if (!anterior || p.i === anterior.i + 1) {
      corrida.push(p)
      continue
    }
    fecharCorrida()
    trechos.push({ chave: `v-${anterior.bucket.chave}`, pontos: [anterior, p], continuo: false })
    corrida = [p]
  }
  fecharCorrida()

  return trechos
}

/** `true` quando há série para desenhar (≥ 2 buckets COM dado) — senão, estado vazio. */
export function temSerieTemporal(janela: BucketTempo[]): boolean {
  return janela.filter((b) => b.total > 0).length >= LINHA_MIN_PONTOS
}

export interface Tendencia {
  primeiro: BucketTempo | null
  ultimo: BucketTempo | null
  /** último − primeiro (positivo = autonomia subiu). `null` se não dá para comparar. */
  deltaAutonomia: number | null
  /** último − primeiro (positivo = complexidade SUBIU = ficou mais custosa). `null` = sem base. */
  deltaClasse: number | null
  /** Texto do rodapé, DERIVADO DOS DADOS (nunca fixo — spec 02, Lacuna L). */
  texto: string
}

/** Abaixo disso a variação não é reportada como subida/queda. */
const LIMIAR_TENDENCIA = 0.2

/**
 * Lacuna L (resolvida na spec): o rodapé do protótipo afirmava "autonomia sobe enquanto a
 * complexidade típica cai" — mas a linha tracejada dele SUBIA. A legenda NÃO PODE ser fixa:
 * é gerada comparando o primeiro e o último bucket com dado.
 *
 * ⚠ As duas séries têm bases DIFERENTES: autonomia existe em todo bucket com resolução;
 * complexidade só nos buckets com ao menos uma resolução classificada. Nada de `?? 0` — um
 * bucket sem classe NÃO vale O(1); sem base, a tendência de complexidade simplesmente não é
 * afirmada.
 */
export function tendenciaDaLinha(janela: BucketTempo[]): Tendencia {
  const comResolucao = janela.filter((b) => b.total > 0)
  const primeiro = comResolucao[0] ?? null
  const ultimo = comResolucao[comResolucao.length - 1] ?? null

  if (!primeiro || !ultimo || comResolucao.length < LINHA_MIN_PONTOS) {
    return {
      primeiro,
      ultimo,
      deltaAutonomia: null,
      deltaClasse: null,
      texto: 'dados insuficientes para uma tendência',
    }
  }

  const verbo = (delta: number) =>
    Math.abs(delta) < LIMIAR_TENDENCIA ? 'estável' : delta > 0 ? 'sobe' : 'cai'

  // `mediaAutonomia` nunca é null num bucket com `total > 0`.
  const deltaAutonomia = (ultimo.mediaAutonomia as number) - (primeiro.mediaAutonomia as number)

  const comClasse = janela.filter((b) => b.mediaClasse != null)
  const deltaClasse =
    comClasse.length >= LINHA_MIN_PONTOS
      ? (comClasse[comClasse.length - 1].mediaClasse as number) -
        (comClasse[0].mediaClasse as number)
      : null

  const texto =
    deltaClasse == null
      ? `autonomia ${verbo(deltaAutonomia)} · complexidade típica sem base para tendência`
      : `autonomia ${verbo(deltaAutonomia)} · complexidade típica ${verbo(deltaClasse)}`

  return { primeiro, ultimo, deltaAutonomia, deltaClasse, texto }
}

// ════════════════════════════════════════════════════════════════════════════
// 5 · MATRIZ (heatmap autonomia × classe)
// ════════════════════════════════════════════════════════════════════════════

/** alpha mínimo (célula vazia) e amplitude — protótipo, spec 02 §6.2. */
export const MATRIZ_ALPHA_MIN = 0.1
export const MATRIZ_ALPHA_AMPLITUDE = 0.66

/**
 * alpha = 0 ? 0.10 : 0.10 + (contagem / maxContagem) · 0.66 — protótipo, spec 02 §6.2.
 * ✔ Conferido: com maxContagem=3 devolve 0.10 / 0.32 / 0.54 / 0.76 — os quatro alphas do markup.
 * Normalizar pelo máximo (em vez de somar 0.22 por unidade) mantém a escala válida quando as
 * contagens crescem.
 */
export function alphaMatriz(contagem: number, maxContagem: number): number {
  if (contagem <= 0 || maxContagem <= 0) return MATRIZ_ALPHA_MIN
  return arred(MATRIZ_ALPHA_MIN + (contagem / maxContagem) * MATRIZ_ALPHA_AMPLITUDE)
}

export interface CelulaMatriz {
  k: ClasseK
  autonomia: NivelAutonomia
  contagem: number
  /** já pronto para `rgba(cor da classe k, alpha)` via `rgbaDeHex(corDaClasse(k, tema), alpha)`. */
  alpha: number
}

export interface LinhaMatriz {
  k: ClasseK
  curto: string
  /** 5 células, autonomia 1→5 (colunas da esquerda para a direita). */
  celulas: CelulaMatriz[]
  total: number
}

export interface Matriz {
  /** 8 linhas SEMPRE (índice §6, item 8). ⚠ ORDEM: k=0 (O(1)) PRIMEIRO = TOPO da grade. */
  linhas: LinhaMatriz[]
  maxContagem: number
  total: number
}

/**
 * Grade 8 (classe) × 5 (autonomia), contagem por célula (spec 02 §6).
 *
 * ⚠ ATENÇÃO À ORIENTAÇÃO: aqui o TOPO é o mais eficiente (k=0 na primeira linha) — spec 02
 * §6.1, "uma linha por classe (topo = mais eficiente)". É o INVERSO da Carta, onde O(1) fica
 * na base. Não "corrija" isto: é o desenho.
 *
 * INVARIANTE (spec 02 §6.3): a soma de cada linha == a contagem da mesma classe no Espectro.
 * Os dois gráficos são projeções do mesmo dataset e têm de bater sempre.
 */
export function montarMatriz(pontos: PontoPlotavel[]): Matriz {
  const contagens: number[][] = CLASSES.map(() => new Array<number>(TOTAL_AUTONOMIA).fill(0))
  for (const p of pontos) contagens[p.k][p.autonomia - 1]++

  const max = Math.max(0, ...contagens.flat())

  const linhas: LinhaMatriz[] = CLASSES.map((c) => {
    const linha = contagens[c.k]
    return {
      k: c.k,
      curto: c.curto,
      celulas: linha.map((contagem, i) => ({
        k: c.k,
        autonomia: (i + 1) as NivelAutonomia,
        contagem,
        alpha: alphaMatriz(contagem, max),
      })),
      total: linha.reduce((s, v) => s + v, 0),
    }
  })

  return { linhas, maxContagem: max, total: pontos.length }
}
