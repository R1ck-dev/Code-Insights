/*
 * ESCALAS — a geometria compartilhada dos 5 gráficos.
 *
 * Toda fórmula aqui foi CONFERIDA contra o markup do protótipo (spec 02 diz onde).
 * Cada bloco cita a origem: "protótipo, spec 02 §X". Onde o protótipo não definia nada,
 * a decisão está marcada como DECISÃO e diz de onde veio (§6-A da entrevista, ou minha).
 *
 * Nada aqui importa React. É matemática pura — testável, e os 5 gráficos consomem igual.
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
  type NivelAutonomia,
  type PontoBase,
  type PontoPlotavel,
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

const GRAU = Math.PI / 180

export function grausParaRad(graus: number): number {
  return graus * GRAU
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

// ── Dedup / jitter de pontos coincidentes ───────────────────────────────────
// Carta e Matriz têm domínio DISCRETO (5 autonomias × 8 classes = 40 posições). Duas
// resoluções na mesma célula caem no MESMO pixel e uma esconde a outra — o que apagaria
// dado. Solução: espalhar o grupo num anel minúsculo em torno da posição real.
//
// DECISÃO (minha; o protótipo não tem pontos coincidentes): anel de raio 5px (o núcleo
// da estrela tem r=2.6 → dois núcleos a 5px de distância angular não se tocam), primeiro
// ponto no topo (-90°) e o resto em sentido horário. Grupos com mais de 8 pontos abrem
// anéis concêntricos (raio × 2, × 3…), 8 por anel.

export const JITTER_RAIO = 5
export const JITTER_POR_ANEL = 8

export interface Deslocamento {
  dx: number
  dy: number
}

/**
 * Deslocamentos PARALELOS ao array de entrada (índice a índice). `chaves[i]` identifica a
 * posição lógica do ponto `i` (ex.: `` `${p.autonomia}:${p.k}` ``). Pontos sozinhos na sua
 * célula recebem `{dx:0, dy:0}` — a posição exata é preservada quando não há colisão.
 *
 *   const desl = deslocamentosCoincidentes(pontos.map((p) => `${p.autonomia}:${p.k}`))
 *   const cx = xDaAutonomia(p.autonomia) + desl[i].dx
 */
export function deslocamentosCoincidentes(chaves: string[], raio = JITTER_RAIO): Deslocamento[] {
  const grupos = new Map<string, number[]>()
  chaves.forEach((chave, i) => {
    const g = grupos.get(chave)
    if (g) g.push(i)
    else grupos.set(chave, [i])
  })

  const saida: Deslocamento[] = chaves.map(() => ({ dx: 0, dy: 0 }))
  for (const indices of grupos.values()) {
    if (indices.length < 2) continue
    indices.forEach((idx, j) => {
      const anel = Math.floor(j / JITTER_POR_ANEL) + 1
      const naVolta = Math.min(indices.length - (anel - 1) * JITTER_POR_ANEL, JITTER_POR_ANEL)
      const posicao = j % JITTER_POR_ANEL
      const theta = grausParaRad(-90 + posicao * (360 / naVolta))
      const r = raio * anel
      saida[idx] = { dx: arred(r * Math.cos(theta)), dy: arred(r * Math.sin(theta)) }
    })
  }
  return saida
}

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

/** Posição exata (sem jitter) de um ponto na Carta. */
export function posicaoNaCarta(p: PontoPlotavel): Ponto2D {
  return { x: xDaAutonomia(p.autonomia), y: yDaClasse(p.k) }
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

// ── Callout (compartilhado com as Órbitas) ──────────────────────────────────

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
// 2 · ÓRBITAS (polar) — raio = autonomia · ÂNGULO = TEMPO
// ════════════════════════════════════════════════════════════════════════════

/** `viewBox="0 0 300 300"`, centro (150,150) — protótipo, spec 02 §3. */
export const ORBITA_VIEWBOX = { largura: 300, altura: 300 } as const
export const ORBITA_CENTRO: Ponto2D = { x: 150, y: 150 }

/** r(a) = 26·a → 26, 52, 78, 104, 130. Conferido nos 11 pontos do protótipo (spec 02 §3.1). */
export const ORBITA_PASSO_RAIO = 26
export const ORBITA_RAIO_MAX = ORBITA_PASSO_RAIO * AUTONOMIA_MAX // 130

/**
 * Raio pelo nível de autonomia. Centro = dependência total (autonomia 1 mais perto do centro,
 * 5 no anel externo). Com o `rMax` padrão (130) devolve exatamente `26·a` do protótipo.
 */
export function raioPorAutonomia(a: number, rMax: number = ORBITA_RAIO_MAX): number {
  const nivel = limitar(Math.round(a), AUTONOMIA_MIN, AUTONOMIA_MAX)
  return (nivel / TOTAL_AUTONOMIA) * rMax
}

/**
 * ÂNGULO = TEMPO (§6-A, Lacuna 3 — decisão da entrevista, MANDA sobre a spec 02, que
 * distribuía a olho): as Órbitas são o RELÓGIO do portfólio.
 *
 *   θᵢ = -90° + i · (360° / n)
 *
 * `i` = índice do ponto na ordem cronológica ASCENDENTE (= o índice no array `dataset.pontos`,
 * que `montarDataset` já entrega ordenado). 12h = a resolução mais ANTIGA; gira em sentido
 * horário (y cresce para baixo no SVG, então ângulo crescente = horário). Índices distintos →
 * ângulos distintos → nenhum ponto se sobrepõe.
 *
 * Retorna RADIANOS (pronto para `Math.cos`/`Math.sin`).
 */
export function anguloPorTempo(i: number, n: number): number {
  if (n <= 1) return grausParaRad(-90) // ponto único: no topo
  return grausParaRad(-90 + i * (360 / n))
}

/** O mesmo ângulo em graus — para `rotate()` de rótulos ou depuração. */
export function anguloPorTempoGraus(i: number, n: number): number {
  return n <= 1 ? -90 : -90 + i * (360 / n)
}

/**
 * Posição cartesiana de um ponto na órbita.
 * cx = 150 + r·cos(θ) · cy = 150 + r·sin(θ) (convenção SVG: y cresce para baixo).
 */
export function posicaoOrbital(i: number, n: number, autonomia: number): Ponto2D {
  const r = raioPorAutonomia(autonomia)
  const theta = anguloPorTempo(i, n)
  return {
    x: arred(ORBITA_CENTRO.x + r * Math.cos(theta)),
    y: arred(ORBITA_CENTRO.y + r * Math.sin(theta)),
  }
}

export interface AnelOrbita {
  a: NivelAutonomia
  r: number
  /** o anel externo (a=5) é mais forte: `graf-eixo` nos internos, `line` no externo. */
  externo: boolean
  /** y do rótulo do anel, no topo dele: `150 − 26a + 3.5` (protótipo, spec 02 §3.2). */
  rotuloY: number
}

/** Os 5 anéis. O protótipo rotula só 1, 4 e 5 — rotular 2 e 3 é opcional (polui). */
export function aneisOrbita(): AnelOrbita[] {
  const aneis: AnelOrbita[] = []
  for (let a = AUTONOMIA_MIN; a <= AUTONOMIA_MAX; a++) {
    const r = raioPorAutonomia(a)
    aneis.push({
      a: a as NivelAutonomia,
      r,
      externo: a === AUTONOMIA_MAX,
      rotuloY: arred(ORBITA_CENTRO.y - r + 3.5),
    })
  }
  return aneis
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
// 4 · LINHA (evolução temporal) — buckets mensais
// ════════════════════════════════════════════════════════════════════════════

/** `viewBox="0 0 400 220"` — protótipo, spec 02 §5. */
export const LINHA_VIEWBOX = { largura: 400, altura: 220 } as const

/** Dados em x ∈ [40, 384] (largura 344); banda vertical y ∈ [60, 200] (protótipo, §5.2). */
export const LINHA_X0 = 40
export const LINHA_LARGURA = 344
export const LINHA_Y_TOPO = 60
export const LINHA_Y_BASE = 200

/** Janela: os últimos 8 meses do intervalo de atividade (protótipo desenha 8 buckets). */
export const LINHA_MAX_BUCKETS = 8
/** Com menos de 2 buckets COM DADO não há série — a tela mostra o estado vazio (Lacuna M). */
export const LINHA_MIN_PONTOS = 2

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

export interface BucketMes {
  ano: number
  /** 1..12 (mês de calendário, não índice). */
  mes: number
  /** `aaaa-mm` — chave estável. */
  chave: string
  /** 'jan', 'fev'… — rótulo do eixo X. */
  rotulo: string
  inicio: Date
  /** TODAS as resoluções do mês — inclusive as sem métrica (autonomia independe da linguagem). */
  resolucoes: PontoBase[]
  /** Só as com classe de tempo: a série de COMPLEXIDADE sai daqui. */
  comMetrica: PontoBase[]
  /** = `resolucoes.length`. Zero = mês SEM RESOLUÇÃO (o aluno não enviou nada). */
  total: number
  /** Quantas do mês não têm classe de complexidade. `total > 0 && semMetrica === total` é
   *  um mês de trabalho SEM métrica — jamais um mês "vazio". */
  semMetrica: number
  /** média de autonomia do mês sobre TODAS as resoluções; `null` só no mês sem resolução. */
  mediaAutonomia: number | null
  /** média de `k` (classe de tempo) sobre as COM métrica; `null` quando nenhuma tem classe. */
  mediaClasse: number | null
}

function media(valores: number[]): number | null {
  if (valores.length === 0) return null
  return valores.reduce((s, v) => s + v, 0) / valores.length
}

/**
 * Buckets MENSAIS (spec 02 §5 + Lacuna M).
 *
 * ⚠ Recebe `PontoBase[]` = `dataset.todas`, NÃO `dataset.pontos`. Um mês em que o aluno
 * submeteu 3 resoluções em Python tem 3 resoluções — ele não é um mês "sem resolução".
 * A autonomia é autodeclarada e independe da linguagem (§4.4): a série neutra atravessa
 * o mês; a série de complexidade é que se interrompe.
 *
 * DECISÃO (minha, combinando as propostas da spec):
 *   1. O intervalo é CONTÍNUO do 1º ao último mês com atividade — meses vazios NÃO somem,
 *      ocupam seu `x` (o tempo não anda mais devagar porque o aluno parou de enviar).
 *   2. Se o intervalo passar de `LINHA_MAX_BUCKETS` (8), fica com os 8 meses MAIS RECENTES.
 *   3. Mês sem resolução → `mediaAutonomia` = `null` → não vira ponto; a série QUEBRA ali
 *      (use `segmentosDaSerie`, não uma polyline única). Mês com resolução mas sem métrica →
 *      só `mediaClasse` é `null`: a linha de complexidade quebra, a de autonomia não.
 *
 * Recebe as resoluções já em ordem cronológica ascendente (garantia de `montarDataset`).
 */
export function bucketsMensais(
  resolucoes: PontoBase[],
  maxBuckets = LINHA_MAX_BUCKETS,
): BucketMes[] {
  if (resolucoes.length === 0) return []

  // índice absoluto de mês: ano*12 + (mês-1) — aritmética de calendário sem fuso.
  const indiceDe = (d: Date) => d.getFullYear() * 12 + d.getMonth()
  const porMes = new Map<number, PontoBase[]>()
  let min = Number.POSITIVE_INFINITY
  let max = Number.NEGATIVE_INFINITY

  for (const p of resolucoes) {
    const idx = indiceDe(p.submetidaEm)
    min = Math.min(min, idx)
    max = Math.max(max, idx)
    const g = porMes.get(idx)
    if (g) g.push(p)
    else porMes.set(idx, [p])
  }

  const inicio = Math.max(min, max - maxBuckets + 1) // janela: os N meses mais recentes
  const buckets: BucketMes[] = []
  for (let idx = inicio; idx <= max; idx++) {
    const ano = Math.floor(idx / 12)
    const mes0 = idx % 12
    const doMes = porMes.get(idx) ?? []
    const comMetrica = doMes.filter((p) => p.k != null)
    buckets.push({
      ano,
      mes: mes0 + 1,
      chave: `${ano}-${doisDigitos(mes0 + 1)}`,
      rotulo: MESES_CURTOS[mes0],
      inicio: new Date(ano, mes0, 1),
      resolucoes: doMes,
      comMetrica,
      total: doMes.length,
      semMetrica: doMes.length - comMetrica.length,
      mediaAutonomia: media(doMes.map((p) => p.autonomia)),
      mediaClasse: media(comMetrica.map((p) => p.k as number)),
    })
  }
  return buckets
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
  bucket: BucketMes
}

/** Os pontos de uma série (só os buckets COM dado). Meses vazios não viram ponto. */
export function pontosDaSerie(buckets: BucketMes[], serie: SerieLinha): PontoSerie[] {
  const n = buckets.length
  const saida: PontoSerie[] = []
  buckets.forEach((bucket, i) => {
    const valor = serie === 'autonomia' ? bucket.mediaAutonomia : bucket.mediaClasse
    if (valor == null) return // mês sem resolução: sem ponto (e a linha quebra aqui)
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
 * A série quebrada em SEGMENTOS de meses consecutivos com dado — uma `<polyline>` por
 * segmento. Uma polyline única "pularia" o buraco ligando dois meses não adjacentes, o que
 * mentiria sobre o intervalo (spec 02, Lacuna M).
 * Segmento com 1 ponto é mantido (o gráfico desenha só o marcador).
 */
export function segmentosDaSerie(buckets: BucketMes[], serie: SerieLinha): PontoSerie[][] {
  const pontos = pontosDaSerie(buckets, serie)
  const segmentos: PontoSerie[][] = []
  let atual: PontoSerie[] = []

  for (const p of pontos) {
    const anterior = atual[atual.length - 1]
    if (anterior && p.i !== anterior.i + 1) {
      segmentos.push(atual)
      atual = []
    }
    atual.push(p)
  }
  if (atual.length > 0) segmentos.push(atual)
  return segmentos
}

/** `true` quando há série para desenhar (≥ 2 meses COM dado) — senão, estado vazio. */
export function temSerieTemporal(buckets: BucketMes[]): boolean {
  return buckets.filter((b) => b.total > 0).length >= LINHA_MIN_PONTOS
}

export interface Tendencia {
  primeiro: BucketMes | null
  ultimo: BucketMes | null
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
 * ⚠ As duas séries têm bases DIFERENTES: autonomia existe em todo mês com resolução;
 * complexidade só nos meses com ao menos uma resolução classificada. Nada de `?? 0` — um
 * mês sem classe NÃO vale O(1); sem base, a tendência de complexidade simplesmente não é
 * afirmada.
 */
export function tendenciaDaLinha(buckets: BucketMes[]): Tendencia {
  const comResolucao = buckets.filter((b) => b.total > 0)
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

  const comClasse = buckets.filter((b) => b.mediaClasse != null)
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
