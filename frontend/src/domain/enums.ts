/*
 * Metadados de apresentação dos enums do domínio: rótulos pt-BR amigáveis,
 * cores (linguagem, complexidade), ícones e a natureza (exata/estimada) das
 * métricas. Centraliza o "de-para" para nenhuma tela repetir constantes cruas.
 */
import {
  Braces,
  Code,
  Database,
  GitBranch,
  Hash,
  Layers,
  RefreshCw,
  SlidersHorizontal,
  type LucideIcon,
} from 'lucide-react'
import type {
  CategoriaConceito,
  LinguagemProgramacao,
  Role,
  StatusConta,
  TipoMetrica,
  Visibilidade,
} from '@/types/api'

export type Tone = 'brand' | 'success' | 'warning' | 'danger' | 'info' | 'neutral'

// ---- Role ----
export const ROLE_LABEL: Record<Role, string> = {
  ALUNO: 'Aluno',
  PESQUISADOR: 'Pesquisador',
  ADMIN: 'Administrador',
}

// ---- Status da conta ----
export const STATUS_CONTA_META: Record<StatusConta, { label: string; tone: Tone }> = {
  PENDENTE_VERIFICACAO: { label: 'Aguardando verificação', tone: 'warning' },
  ATIVO: { label: 'Ativa', tone: 'success' },
  INATIVO: { label: 'Inativa', tone: 'neutral' },
  SUSPENSO: { label: 'Suspensa', tone: 'danger' },
}

// ---- Visibilidade ----
export const VISIBILIDADE_META: Record<
  Visibilidade,
  { label: string; tone: Tone; icon: 'globe' | 'lock' }
> = {
  PUBLICO: { label: 'Público', tone: 'success', icon: 'globe' },
  PRIVADO: { label: 'Privado', tone: 'neutral', icon: 'lock' },
}

// ---- Linguagens ----
export interface LinguagemMeta {
  value: LinguagemProgramacao
  label: string
  color: string
  /** chave usada pelo highlighter do CodeBlock */
  codeLang: 'java' | 'python' | 'cpp' | 'javascript' | 'c'
}

export const LINGUAGENS: LinguagemMeta[] = [
  { value: 'JAVA', label: 'Java', color: '#E76F00', codeLang: 'java' },
  { value: 'PYTHON', label: 'Python', color: '#4B8BBE', codeLang: 'python' },
  { value: 'CPP', label: 'C++', color: '#4C93D6', codeLang: 'cpp' },
  { value: 'JAVASCRIPT', label: 'JavaScript', color: '#E9C500', codeLang: 'javascript' },
  { value: 'C', label: 'C', color: '#659AD2', codeLang: 'c' },
]

export const LINGUAGEM_META: Record<LinguagemProgramacao, LinguagemMeta> = Object.fromEntries(
  LINGUAGENS.map((l) => [l.value, l]),
) as Record<LinguagemProgramacao, LinguagemMeta>

/** Só Java gera métricas de complexidade hoje (motor JavaParser). */
export const LINGUAGEM_COM_METRICAS: LinguagemProgramacao = 'JAVA'

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
export const TIPO_METRICA_META: Record<
  TipoMetrica,
  { nome: string; sub: string; natureza: 'exata' | 'estimada' }
> = {
  COMPLEXIDADE_CICLOMATICA: {
    nome: 'Complexidade ciclomática',
    sub: 'McCabe',
    natureza: 'exata',
  },
  BIG_O_TEMPO: {
    nome: 'Complexidade de tempo',
    sub: 'Big O · heurística AST',
    natureza: 'estimada',
  },
  COMPLEXIDADE_ESPACO: {
    nome: 'Complexidade de espaço',
    sub: 'heurística AST',
    natureza: 'estimada',
  },
}

// ---- Escala de complexidade (verde → vermelho) ----
const CX_HEX = [
  '#2FB863', // O(1)
  '#59C36A', // O(log n)
  '#9CC15A', // O(n)
  '#E0A21E', // O(n log n)
  '#EC8A3C', // O(n^2)
  '#F0563F', // O(n^3)
  '#E23D5A', // O(2^n)
  '#C2304E', // O(n!)
]
const CX_UNKNOWN = '#666B7A'

const ROTULO_ORDINAL: Record<string, number> = {
  'O(1)': 0,
  'O(log n)': 1,
  'O(n)': 2,
  'O(n log n)': 3,
  'O(n^2)': 4,
  'O(n^3)': 5,
  'O(2^n)': 6,
  'O(n!)': 7,
}

/** Cor da escala pelo ordinal (valor do backend para Big O de tempo/espaço). */
export function complexityHexByOrdinal(ordinal: number): string {
  return ordinal >= 0 && ordinal < CX_HEX.length ? CX_HEX[ordinal] : CX_UNKNOWN
}

/** Cor da escala a partir do rótulo textual (ex.: "O(n log n)"). */
export function complexityHexByRotulo(rotulo: string): string {
  const ord = ROTULO_ORDINAL[rotulo]
  return ord === undefined ? CX_UNKNOWN : CX_HEX[ord]
}

/** "O(n^2)" → "O(n²)" para exibição. */
export function prettyBigO(rotulo: string): string {
  return rotulo
    .replace(/\^2/g, '²')
    .replace(/\^3/g, '³')
    .replace(/\^n/g, 'ⁿ')
}
