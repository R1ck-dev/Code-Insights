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

/** Só Java gera métricas de complexidade hoje (motor JavaParser). */
export const LINGUAGEM_COM_METRICAS: LinguagemProgramacao = 'JAVA'

/** Nota permanente onde a métrica não existe por causa da linguagem (§4.4). */
export const NOTA_METRICAS_SO_JAVA = 'Métricas de complexidade hoje só para Java.'

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
