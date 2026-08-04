/*
 * Metadados de apresentação dos enums do domínio: rótulos pt-BR, cores de
 * linguagem, ícones (lucide) e a natureza (MEDIDO/ESTIMADO) das métricas.
 * Centraliza o "de-para" para nenhuma tela repetir constantes cruas.
 *
 * O colormap de complexidade — a ÚNICA cor do sistema Órbita — vive em
 * `@/domain/complexidade` e é re-exportado aqui por conveniência.
 */
import {
  Braces,
  Code,
  Database,
  GitBranch,
  Globe,
  Hash,
  Layers,
  Lock,
  RefreshCw,
  SlidersHorizontal,
  type LucideIcon,
} from 'lucide-react'
import type {
  CategoriaConceito,
  LinguagemProgramacao,
  NivelConfianca,
  Role,
  StatusConta,
  TipoMetrica,
  Visibilidade,
} from '@/types/api'
import type { Confianca } from '@/domain/complexidade'

export * from '@/domain/complexidade'

/**
 * Tons semânticos do sistema. NÃO existe tom de marca — a única cor de acento
 * do Órbita é o colormap de complexidade (`@/domain/complexidade`).
 * `info` é neutro por definição (§2.3: "Info / calculando = ink + spinner, sem cor").
 */
export type Tone = 'neutro' | 'sucesso' | 'atencao' | 'erro' | 'info'

// ---- Role ----
export const ROLE_LABEL: Record<Role, string> = {
  ALUNO: 'Aluno',
  PESQUISADOR: 'Pesquisador',
  ADMIN: 'Administrador',
}

// ---- Status da conta ----
export const STATUS_CONTA_META: Record<StatusConta, { label: string; tone: Tone }> = {
  PENDENTE_VERIFICACAO: { label: 'Aguardando verificação', tone: 'atencao' },
  ATIVO: { label: 'Ativa', tone: 'sucesso' },
  INATIVO: { label: 'Inativa', tone: 'neutro' },
  SUSPENSO: { label: 'Suspensa', tone: 'erro' },
}

// ---- Visibilidade ----
export const VISIBILIDADE_META: Record<
  Visibilidade,
  { label: string; tone: Tone; icon: 'globe' | 'lock' }
> = {
  PUBLICO: { label: 'Público', tone: 'sucesso', icon: 'globe' },
  PRIVADO: { label: 'Privado', tone: 'neutro', icon: 'lock' },
}

/** Ícone lucide da visibilidade (`strokeWidth={2}` em todo o app). */
export const VISIBILIDADE_ICONE: Record<Visibilidade, LucideIcon> = {
  PUBLICO: Globe,
  PRIVADO: Lock,
}

// ---- Linguagens ----
export interface LinguagemMeta {
  value: LinguagemProgramacao
  label: string
  /** cor do ponto do chip (7px) / header do CodeBlock (9px). Idêntica nos dois temas. */
  color: string
  /** chave usada pelo highlighter do CodeBlock */
  codeLang: 'java' | 'python' | 'cpp' | 'javascript' | 'c'
}

export const LINGUAGENS: LinguagemMeta[] = [
  { value: 'JAVA', label: 'Java', color: '#E76F00', codeLang: 'java' },
  { value: 'PYTHON', label: 'Python', color: '#4B8BBE', codeLang: 'python' },
  { value: 'CPP', label: 'C++', color: '#4C93D6', codeLang: 'cpp' },
  // ⚠ JavaScript é #E9C500, nunca #D3AE3F (essa é a classe O(n log n) do colormap).
  { value: 'JAVASCRIPT', label: 'JavaScript', color: '#E9C500', codeLang: 'javascript' },
  { value: 'C', label: 'C', color: '#659AD2', codeLang: 'c' },
]

export const LINGUAGEM_META: Record<LinguagemProgramacao, LinguagemMeta> = Object.fromEntries(
  LINGUAGENS.map((l) => [l.value, l]),
) as Record<LinguagemProgramacao, LinguagemMeta>

/** Cor de linguagem desconhecida — token `steel`. */
export const LINGUAGEM_COR_FALLBACK = '#8FA6C9'

/**
 * O que o motor sabe medir em cada linguagem. **O suporte pode ser parcial**, e não um sim/não:
 * contar pontos de decisão é contagem, enquanto inferir Big O exige um modelo de custo sobre a
 * estrutura inteira — uma linguagem nova entra pela primeira antes de ganhar a segunda, e foi assim
 * que C entrou. Hoje Java e C produzem as três.
 *
 * Espelha `AnalisadorMetricas.metricasSuportadas` no backend. Um dia isto deve vir de lá (o
 * `ContagemPorLinguagemDTO` já traz o conjunto por linguagem); enquanto for constante, mudar o
 * motor exige mudar esta tabela junto.
 */
export const METRICAS_POR_LINGUAGEM: Record<LinguagemProgramacao, TipoMetrica[]> = {
  JAVA: ['BIG_O_TEMPO', 'COMPLEXIDADE_ESPACO', 'COMPLEXIDADE_CICLOMATICA'],
  C: ['BIG_O_TEMPO', 'COMPLEXIDADE_ESPACO', 'COMPLEXIDADE_CICLOMATICA'],
  CPP: [],
  PYTHON: [],
  JAVASCRIPT: [],
}

/** A linguagem produz classe de tempo — o que a Carta, a Linha e a Matriz plotam. */
export function temClasseDeTempo(linguagem: LinguagemProgramacao): boolean {
  return METRICAS_POR_LINGUAGEM[linguagem].includes('BIG_O_TEMPO')
}

/** A linguagem produz alguma métrica. Falso aqui é o único caso de "sem analisador" de verdade. */
export function temAlgumaMetrica(linguagem: LinguagemProgramacao): boolean {
  return METRICAS_POR_LINGUAGEM[linguagem].length > 0
}

/** As linguagens que o motor de fato analisa, derivadas da tabela — nunca uma lista à parte. */
export const LINGUAGENS_ANALISADAS = (
  Object.keys(METRICAS_POR_LINGUAGEM) as LinguagemProgramacao[]
).filter(temAlgumaMetrica)

/** `[A]` → "A" · `[A, B]` → "A e B" · `[A, B, C]` → "A, B e C". */
export function listaEmPortugues(itens: string[]): string {
  if (itens.length <= 1) return itens[0] ?? ''
  return `${itens.slice(0, -1).join(', ')} e ${itens[itens.length - 1]}`
}

/** Os rótulos das linguagens analisadas, prontos para entrar numa frase. */
export function rotulosDasLinguagensAnalisadas(): string {
  return listaEmPortugues(LINGUAGENS_ANALISADAS.map((linguagem) => LINGUAGEM_META[linguagem].label))
}

/**
 * Nota permanente ao lado dos gráficos, que plotam **classe de tempo** (§4.4).
 *
 * O texto é **derivado da tabela**, e não escrito à mão. Já disse "só para Java" duas vezes na vida
 * deste arquivo, e as duas vezes envelheceu no mesmo commit em que uma linguagem entrou no motor —
 * apagando da tela um número que a plataforma tinha passado a medir. Agora não tem como envelhecer:
 * acrescentar uma linguagem à tabela reescreve a frase.
 */
export const NOTA_LINGUAGENS_ANALISADAS = `O motor analisa ${rotulosDasLinguagensAnalisadas()}; nas demais linguagens, ainda não.`

/**
 * Teto de confiança do motor por linguagem. Em C a estrutura do código é reconhecida por forma, sem
 * parse completo da linguagem, e o backend nunca grava `ALTA` — ver `AnalisadorDeC`.
 *
 * A tela precisa saber disto porque existe um filtro "só confiança ALTA": sem o aviso, um
 * pesquisador ligaria o filtro num piloto majoritariamente em C e leria um zero como "não há dado",
 * quando na verdade é "este corte exclui esta linguagem inteira, por construção".
 */
export const TETO_DE_CONFIANCA: Partial<Record<LinguagemProgramacao, NivelConfianca>> = {
  C: 'MEDIA',
}

export function nuncaAtingeConfiancaAlta(linguagem: LinguagemProgramacao): boolean {
  return TETO_DE_CONFIANCA[linguagem] !== undefined
}

export const NOTA_TETO_DE_CONFIANCA =
  'Em C o motor lê a estrutura por forma, sem parse completo da linguagem: a confiança não passa de MÉDIA.'

/**
 * Nota por linguagem, para as telas de uma resolução só. Separada da anterior porque aqui dá para
 * ser específico sobre o que aquela linguagem ganha e o que não ganha.
 */
export function notaDeCobertura(linguagem: LinguagemProgramacao): string {
  if (!temAlgumaMetrica(linguagem)) return 'O motor ainda não analisa esta linguagem.'
  if (!temClasseDeTempo(linguagem))
    return 'Nesta linguagem o motor mede a complexidade ciclomática; Big O e espaço, ainda não.'
  return nuncaAtingeConfiancaAlta(linguagem) ? NOTA_TETO_DE_CONFIANCA : ''
}

// ---- Categorias de snippet ----
export interface CategoriaMeta {
  value: CategoriaConceito
  label: string
  icon: LucideIcon
}

export const CATEGORIAS: CategoriaMeta[] = [
  { value: 'ESTRUTURA_DADOS', label: 'Estruturas de dados', icon: Database },
  { value: 'RECURSAO', label: 'Recursão', icon: RefreshCw },
  { value: 'ORDENACAO', label: 'Ordenação', icon: SlidersHorizontal },
  { value: 'GRAFOS', label: 'Grafos', icon: GitBranch },
  { value: 'PROGRAMACAO_DINAMICA', label: 'Programação dinâmica', icon: Layers },
  { value: 'STRINGS', label: 'Strings', icon: Code },
  { value: 'MATEMATICA', label: 'Matemática', icon: Hash },
]

export const CATEGORIA_META: Record<CategoriaConceito, CategoriaMeta> = Object.fromEntries(
  CATEGORIAS.map((c) => [c.value, c]),
) as Record<CategoriaConceito, CategoriaMeta>

export const SNIPPET_FALLBACK_ICON = Braces

// ---- Métricas ----
export interface TipoMetricaMeta {
  /** rótulo do tile, já em caixa alta (mono 10.5px). */
  rotulo: string
  /** nome por extenso — listas, tooltips, diálogo "explicar métricas". */
  nome: string
  /** método, sob o rótulo do tile (mono 9px). */
  metodo: string
  /** MEDIDO = contagem direta no AST · ESTIMADO = inferido (prefixo `≈`). */
  confianca: Confianca
  /** o valor é uma classe do colormap (0..7)? A ciclomática não é — e não tem barra. */
  ehClasseBigO: boolean
}

/**
 * A NATUREZA da métrica — a ÚNICA fonte de MEDIDO × ≈ ESTIMADO.
 *
 * ⚠ Não derive isto de `NivelConfianca` (ALTA/MEDIA/BAIXA). São dois eixos distintos:
 *   · aqui: a métrica é contada (MEDIDO) ou inferida (≈ ESTIMADO)? É propriedade do TIPO.
 *     Big-O de tempo/espaço é SEMPRE estimado — inferir a complexidade de um código
 *     arbitrário é indecidível no caso geral. `ALTA` significa "o motor reconheceu todos os
 *     construtos", NÃO "o Big-O foi medido".
 *   · `NivelConfianca`: quanta confiança o motor tem no valor que ele mesmo ESTIMOU
 *     (ver `CONFIANCA_MOTOR_LABEL`). Vira texto ao lado do valor, nunca preenche marcador.
 */
export const TIPO_METRICA_META: Record<TipoMetrica, TipoMetricaMeta> = {
  COMPLEXIDADE_CICLOMATICA: {
    rotulo: 'CICLOMÁTICA',
    nome: 'Complexidade ciclomática',
    metodo: 'McCabe',
    confianca: 'MEDIDO',
    ehClasseBigO: false,
  },
  BIG_O_TEMPO: {
    rotulo: 'TEMPO',
    nome: 'Complexidade de tempo',
    metodo: 'Big O · AST',
    confianca: 'ESTIMADO',
    ehClasseBigO: true,
  },
  COMPLEXIDADE_ESPACO: {
    rotulo: 'ESPAÇO',
    nome: 'Complexidade de espaço',
    metodo: 'Big O · AST',
    confianca: 'ESTIMADO',
    ehClasseBigO: true,
  },
}

/** Atalho: a natureza (sempre ≈ ESTIMADO) da classe de complexidade de TEMPO. */
export const CONFIANCA_BIG_O: Confianca = TIPO_METRICA_META.BIG_O_TEMPO.confianca

// ---- Confiança do MOTOR (eixo secundário) ----

/**
 * Quanto o motor confia no valor que ele PRÓPRIO estimou (`NivelConfianca` do backend).
 * ALTA = reconheceu todos os construtos · MEDIA = assumiu ao menos um default conservador ·
 * BAIXA = não conseguiu classificar. NÃO é MEDIDO/ESTIMADO — ver `TIPO_METRICA_META`.
 */
export const CONFIANCA_MOTOR_LABEL: Record<NivelConfianca, string> = {
  ALTA: 'alta',
  MEDIA: 'média',
  BAIXA: 'baixa',
}

/** `"confiança do motor: alta"` — ou `null` quando o backend não informou. */
export function rotuloConfiancaMotor(nivel: NivelConfianca | null | undefined): string | null {
  if (!nivel) return null
  return `confiança do motor: ${CONFIANCA_MOTOR_LABEL[nivel]}`
}
