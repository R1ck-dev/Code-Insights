/*
 * COLORMAP DE COMPLEXIDADE — a única fonte de cor do sistema Órbita.
 *
 * Verde (eficiente) → vermelho (custoso), 8 classes indexadas por `k` (0..7).
 * Não existe cor de marca: qualquer acento cromático da interface sai daqui.
 * Valores canônicos: docs/design/specs/00-INDICE.md §2.2.
 *
 * O índice vem PRONTO do backend: ClasseComplexidade.ordem = 0..7 e
 * DESCONHECIDO = -1. Não há parser de string — `k = ordem`, direto.
 * `ordem === -1` ou `null` = "sem métrica": não é plotável nem colorível pelo
 * colormap; usa o neutro `soft`.
 */

/** Tema ativo — mesmos literais de `useTheme()` (`@/theme/ThemeProvider`). */
export type Tema = 'dark' | 'light'

/** Índice de uma classe do colormap. `k = ClasseComplexidade.ordem` do backend. */
export type ClasseK = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7

/** Ordem que o backend envia quando não classificou (ou quando não há métrica). */
export const ORDEM_DESCONHECIDA = -1

/** Confiança da métrica, no vocabulário do design (rule 3: nunca esconder incerteza). */
export type Confianca = 'MEDIDO' | 'ESTIMADO'

/** Prefixo obrigatório do valor/rótulo quando a métrica é ESTIMADA. */
export const PREFIXO_ESTIMADO = '≈ '

export interface ClasseComplexidadeMeta {
  k: ClasseK
  /** id do enum do backend (ClasseComplexidade). */
  id: 'O_1' | 'O_LOG_N' | 'O_N' | 'O_N_LOG_N' | 'O_N2' | 'O_N3' | 'O_EXPONENCIAL' | 'O_FATORIAL'
  /** rótulo canônico: 'O(1)' … 'O(n!)' (já com expoentes unicode). */
  canonico: string
  /** rótulo curto de eixo/chip: 'O(1)', 'log n', 'O(n)', 'n log n', 'n²', 'n³', '2ⁿ', 'n!'. */
  curto: string
  /** hex do tema escuro (serve de preenchimento E de tinta de texto). */
  escuro: string
  /** hex do tema claro — PREENCHIMENTO (célula, ponto, quadrado do chip, barra). */
  claro: string
  /** hex do tema claro — TINTA de texto (contraste AA sobre fundo claro). */
  tintaClara: string
}

/** As 8 classes, na ordem do colormap. Índice do array === `k`. */
export const CLASSES: readonly ClasseComplexidadeMeta[] = [
  { k: 0, id: 'O_1',           canonico: 'O(1)',       curto: 'O(1)',    escuro: '#4FB477', claro: '#3E9E63', tintaClara: '#2F7D4F' },
  { k: 1, id: 'O_LOG_N',       canonico: 'O(log n)',   curto: 'log n',   escuro: '#7FBD5C', claro: '#6FA83F', tintaClara: '#587F2F' },
  { k: 2, id: 'O_N',           canonico: 'O(n)',       curto: 'O(n)',    escuro: '#A9BE49', claro: '#9CA82F', tintaClara: '#767F23' },
  { k: 3, id: 'O_N_LOG_N',     canonico: 'O(n log n)', curto: 'n log n', escuro: '#D3AE3F', claro: '#CE9A24', tintaClara: '#A6791C' },
  { k: 4, id: 'O_N2',          canonico: 'O(n²)',      curto: 'n²',      escuro: '#E08A3C', claro: '#D67A24', tintaClara: '#C05B2E' },
  { k: 5, id: 'O_N3',          canonico: 'O(n³)',      curto: 'n³',      escuro: '#DC6A3F', claro: '#C85631', tintaClara: '#A24427' },
  { k: 6, id: 'O_EXPONENCIAL', canonico: 'O(2ⁿ)',      curto: '2ⁿ',      escuro: '#CE4C55', claro: '#BC3540', tintaClara: '#992A33' },
  { k: 7, id: 'O_FATORIAL',    canonico: 'O(n!)',      curto: 'n!',      escuro: '#B23A5E', claro: '#9E2B45', tintaClara: '#7F2237' },
] as const

/** Quantidade de classes (8 células da barra de colormap). */
export const TOTAL_CLASSES = CLASSES.length

/** Maior `k` da escala (O(n!) = 7). Normaliza eixos e barras. */
export const COMPLEXIDADE_ORDEM_MAX = TOTAL_CLASSES - 1

/**
 * Neutro do estado "sem métrica" (token `soft`). NUNCA usar uma cor do colormap
 * para um ponto/valor sem classe — a ausência de medida não é uma medida.
 */
export const COR_SEM_METRICA: Record<Tema, string> = {
  // Espelho do token `--soft` (index.css) — já corrigido para passar AA em texto pequeno.
  dark: '#8A93AD',
  light: '#5F6880',
}

/** Texto de lista/atividade quando não há métrica (§4.4 do índice). */
export const ROTULO_SEM_METRICA = 'sem métrica'

/** Rótulo da classe DESCONHECIDO do backend. */
export const ROTULO_DESCONHECIDO = '?'

/** Alphas das superfícies tonalizadas (chips, callouts) — §2.2 do índice. */
export const ALPHA_FUNDO_TONAL = 0.09
export const ALPHA_BORDA_TONAL = 0.3

// ---- Predicados ----

/** `true` se a ordem é uma classe real do colormap (0..7). */
export function ehClasse(ordem: number | null | undefined): ordem is ClasseK {
  return typeof ordem === 'number' && Number.isInteger(ordem) && ordem >= 0 && ordem <= COMPLEXIDADE_ORDEM_MAX
}

/**
 * `true` se o valor pode ser plotado/colorido pelo colormap.
 * `null`, `undefined` e `-1` (DESCONHECIDO) → `false` (entra em "sem métrica").
 */
export function ehPlotavel(ordem: number | null | undefined): ordem is ClasseK {
  return ehClasse(ordem)
}

/** Metadados completos da classe; `null` quando não há classe. */
export function classeDe(ordem: number | null | undefined): ClasseComplexidadeMeta | null {
  return ehClasse(ordem) ? CLASSES[ordem] : null
}

/** Arredonda e limita uma média de ordens (ex.: complexidade típica) a um `k` válido. */
export function ordemArredondada(media: number | null | undefined): ClasseK | null {
  if (typeof media !== 'number' || Number.isNaN(media) || media < 0) return null
  const k = Math.min(COMPLEXIDADE_ORDEM_MAX, Math.max(0, Math.round(media)))
  return k as ClasseK
}

// ---- Cores ----

/**
 * Cor de PREENCHIMENTO da classe (célula da barra, ponto do gráfico, quadrado do
 * chip, barra do eixo). Sem classe → neutro `soft`.
 */
export function corDaClasse(ordem: number | null | undefined, tema: Tema): string {
  const classe = classeDe(ordem)
  if (!classe) return COR_SEM_METRICA[tema]
  return tema === 'dark' ? classe.escuro : classe.claro
}

/**
 * TINTA de TEXTO da classe (valor do tile, rótulo do chip). No escuro é a mesma
 * cor do preenchimento; no claro é a variante escurecida (contraste AA).
 * Sem classe → neutro `soft`.
 */
export function tintaDaClasse(ordem: number | null | undefined, tema: Tema): string {
  const classe = classeDe(ordem)
  if (!classe) return COR_SEM_METRICA[tema]
  return tema === 'dark' ? classe.escuro : classe.tintaClara
}

/** `#RRGGBB` + alpha → `rgba(r, g, b, a)`. Aceita hex de 3 ou 6 dígitos. */
export function rgbaDeHex(hex: string, alpha: number): string {
  let h = hex.replace('#', '')
  if (h.length === 3) h = h[0] + h[0] + h[1] + h[1] + h[2] + h[2]
  const n = Number.parseInt(h, 16)
  const r = (n >> 16) & 255
  const g = (n >> 8) & 255
  const b = n & 255
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

/** Fundo de superfície tonalizada (chip/callout): `rgba(cor, .09)`. */
export function fundoTonal(
  ordem: number | null | undefined,
  tema: Tema,
  alpha: number = ALPHA_FUNDO_TONAL,
): string {
  return rgbaDeHex(corDaClasse(ordem, tema), alpha)
}

/** Borda de superfície tonalizada (chip/callout): `rgba(cor, .30)`. */
export function bordaTonal(
  ordem: number | null | undefined,
  tema: Tema,
  alpha: number = ALPHA_BORDA_TONAL,
): string {
  return rgbaDeHex(corDaClasse(ordem, tema), alpha)
}

// ---- Rótulos ----

/** 'O(1)' · 'log n' · 'O(n)' · 'n log n' · 'n²' · 'n³' · '2ⁿ' · 'n!' — eixos e chips compactos. */
export function rotuloCurto(ordem: number | null | undefined): string {
  return classeDe(ordem)?.curto ?? ROTULO_DESCONHECIDO
}

/** 'O(1)' … 'O(n!)' — valor de métrica, callout, legenda. */
export function rotuloCanonico(ordem: number | null | undefined): string {
  return classeDe(ordem)?.canonico ?? ROTULO_DESCONHECIDO
}

/** Prefixa `≈ ` quando a métrica é ESTIMADA (rule 3). */
export function comPrefixoEstimado(texto: string, confianca: Confianca): string {
  return confianca === 'ESTIMADO' ? `${PREFIXO_ESTIMADO}${texto}` : texto
}

/**
 * Normaliza um rótulo cru do backend para exibição: 'O(n^2)' → 'O(n²)'.
 * Use `rotuloCanonico(ordem)` sempre que tiver a ordem; isto é o fallback para
 * quando só o texto chegou (ex.: `AtividadeRecenteDTO.complexidadeRotulo`).
 */
export function prettyBigO(rotulo: string): string {
  return rotulo.replace(/\^2/g, '²').replace(/\^3/g, '³').replace(/\^n/g, 'ⁿ')
}
