/*
 * Linha de resolução — lista da tela M (detalhe do desafio) e da tela K (desafio público).
 *
 * A linha inteira é um <Link> de verdade. Slots, na ordem do protótipo:
 *   LangChip · rótulo · AutonomyMeter (sm) · métrica · analisada · data
 *
 * Regras que o componente carrega para nenhuma tela reinventar:
 *  - MÉTRICA SÓ EXISTE PARA JAVA (§4.4): outra linguagem → `sem métrica` em mono 10.5 soft.
 *    Nunca um valor vazio, nunca um `—` solto.
 *  - `analisada === false` (Java) → chip `calculando` com spinner.
 *  - `tempoOrdem === -1` → o motor rodou e NÃO classificou: BigOChip neutro `?` (≠ sem métrica).
 *  - Confiança: `ALTA` = MEDIDO · `MEDIA`/`BAIXA` = ≈ ESTIMADO — a incerteza nunca é escondida.
 *  - Autonomia é NEUTRA (osso/tinta), nunca colormap.
 */
import { Link } from 'react-router-dom'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { BigOChip, LangChip, StatusChip } from '@/components/domain/badges'
import {
  type Confianca,
  LINGUAGEM_COM_METRICAS,
  ROTULO_SEM_METRICA,
} from '@/domain/enums'
import { cn, formatDate, formatDayMonth } from '@/lib/utils'
import type { LinguagemProgramacao, NivelConfianca } from '@/types/api'

/** Confiança do backend → confiança do design. `ALTA` é o único MEDIDO. */
export function confiancaDoNivel(nivel?: NivelConfianca | null): Confianca {
  return nivel === 'ALTA' ? 'MEDIDO' : 'ESTIMADO'
}

/** Texto canônico do estado "sem métrica" (mono 10.5px `soft`). */
export function SemMetrica({ className }: { className?: string }) {
  return (
    <span className={cn('shrink-0 font-mono text-[10.5px] text-soft', className)}>
      {ROTULO_SEM_METRICA}
    </span>
  )
}

export interface MetricaSlotProps {
  linguagem: LinguagemProgramacao
  analisada: boolean
  /** `k` do colormap (0..7) · `-1` desconhecido · `null` sem dado. */
  tempoOrdem?: number | null
  confiancaTempo?: NivelConfianca | null
  /** Chip Big-O tonalizado (fundo/borda na cor da classe). */
  tonal?: boolean
  /** `calculando` → `calc.` (dashboard). */
  compact?: boolean
}

/**
 * O slot de métrica de qualquer lista de resoluções. Reusado pela AtividadeLinha —
 * a decisão "chip Big-O × calculando × sem métrica" mora aqui, e só aqui.
 */
export function MetricaSlot({
  linguagem,
  analisada,
  tempoOrdem,
  confiancaTempo,
  tonal,
  compact,
}: MetricaSlotProps) {
  if (linguagem !== LINGUAGEM_COM_METRICAS) return <SemMetrica />
  if (!analisada) return <StatusChip status="calculando" compact={compact} />
  if (tempoOrdem == null) return <SemMetrica />
  return <BigOChip k={tempoOrdem} confianca={confiancaDoNivel(confiancaTempo)} tonal={tonal} />
}

export interface ResolucaoLinhaProps {
  /** Destino: `/app/resolucoes/:id` (tela M) ou `/u/:autorId/…/resolucoes/:id` (tela K). */
  to: string
  linguagem: LinguagemProgramacao
  /** Índice de Autonomia IA autodeclarado (1–5). */
  autonomia: number
  analisada: boolean
  /** `k` do colormap (0..7) · `-1` desconhecido · `null` sem dado. */
  tempoOrdem?: number | null
  confiancaTempo?: NivelConfianca | null
  /** ISO da submissão. */
  submetidaEm: string
  /** Rótulo curto da tentativa (`Força bruta`, `HashMap otimizado`). Sem ele, cai no fallback mono do ID curto. */
  rotulo?: string | null
  /** `lista` (tela M): linha dentro do painel, hairline no topo a partir da 2ª. `cartao` (tela K): card solto. */
  variant?: 'lista' | 'cartao'
  /** `curta` = `dd/mm` (tela M) · `longa` = `dd/mm/aaaa` (tela K). */
  dataFormato?: 'curta' | 'longa'
  className?: string
}

export function ResolucaoLinha({
  to,
  linguagem,
  autonomia,
  analisada,
  tempoOrdem,
  confiancaTempo,
  submetidaEm,
  rotulo,
  variant = 'lista',
  dataFormato = 'curta',
  className,
}: ResolucaoLinhaProps) {
  const cartao = variant === 'cartao'
  const data = dataFormato === 'longa' ? formatDate(submetidaEm) : formatDayMonth(submetidaEm)

  return (
    <Link
      to={to}
      className={cn(
        'group flex items-center gap-3.5 px-4 py-[13px] transition-colors',
        cartao
          ? 'rounded-ci border border-line bg-panel hover:border-line-strong'
          : 'border-t border-line-soft first:border-t-0 hover:bg-elevated',
        className,
      )}
    >
      <LangChip lang={linguagem} className="text-[11px]" />

      <span className="min-w-0 flex-1 truncate text-[13.5px] text-ink">
        {rotulo || <span className="text-mid">Resolução</span>}
      </span>

      <AutonomyMeter value={autonomia} size="sm" className="hidden shrink-0 sm:inline-flex" />

      <MetricaSlot
        linguagem={linguagem}
        analisada={analisada}
        tempoOrdem={tempoOrdem}
        confiancaTempo={confiancaTempo}
        tonal={cartao}
      />

      {analisada && <StatusChip status="analisada" className="hidden lg:inline-flex" />}

      <time
        dateTime={submetidaEm}
        className="tabular shrink-0 text-right font-mono text-[11px] text-soft"
      >
        {data}
      </time>
    </Link>
  )
}
