import { Check, Globe, Lock } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Spinner } from '@/components/ui/spinner'
import {
  complexityHexByOrdinal,
  complexityHexByRotulo,
  LINGUAGEM_META,
  prettyBigO,
  STATUS_CONTA_META,
  VISIBILIDADE_META,
} from '@/domain/enums'
import type { LinguagemProgramacao, NivelConfianca, StatusConta, Visibilidade } from '@/types/api'
import { cn } from '@/lib/utils'

export function LanguageDot({ linguagem, size = 7 }: { linguagem: LinguagemProgramacao; size?: number }) {
  return (
    <span
      className="inline-block shrink-0 rounded-full"
      style={{ width: size, height: size, background: LINGUAGEM_META[linguagem].color }}
    />
  )
}

/** Chip da linguagem: ponto colorido + nome. */
export function LanguageBadge({
  linguagem,
  className,
}: {
  linguagem: LinguagemProgramacao
  className?: string
}) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-md border border-border bg-input px-2 py-[3px] text-[11.5px] font-semibold text-fg',
        className,
      )}
    >
      <LanguageDot linguagem={linguagem} />
      {LINGUAGEM_META[linguagem].label}
    </span>
  )
}

/** Pílula mono de classe de complexidade, colorida pela escala. */
export function ComplexityBadge({
  rotulo,
  valor,
  className,
}: {
  rotulo: string
  /** ordinal (valor do backend p/ Big O); se ausente, deriva do rótulo. */
  valor?: number
  className?: string
}) {
  const color = valor === undefined ? complexityHexByRotulo(rotulo) : complexityHexByOrdinal(valor)
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-md px-2 py-0.5 font-mono text-[11.5px] font-semibold',
        className,
      )}
      style={{
        color,
        background: `color-mix(in srgb, ${color} 11%, transparent)`,
        boxShadow: `inset 0 0 0 1px color-mix(in srgb, ${color} 30%, transparent)`,
      }}
    >
      {prettyBigO(rotulo)}
    </span>
  )
}

const CONFIANCA_META: Record<NivelConfianca, { label: string; classe: string }> = {
  ALTA: { label: 'confiança alta', classe: 'text-success bg-success/10 ring-success/30' },
  MEDIA: { label: 'confiança média', classe: 'text-warning bg-warning/[.12] ring-warning/30' },
  BAIXA: { label: 'confiança baixa', classe: 'text-danger bg-danger/[.12] ring-danger/30' },
}

/** Pílula que expõe o quanto o motor precisou supor para chegar a um valor estimado. */
export function ConfidenceBadge({
  confianca,
  className,
}: {
  confianca: NivelConfianca
  className?: string
}) {
  const meta = CONFIANCA_META[confianca]
  return (
    <span
      className={cn(
        'shrink-0 rounded-full px-2 py-[3px] font-mono text-[10.5px] font-semibold tracking-wider ring-1 ring-inset',
        meta.classe,
        className,
      )}
    >
      {meta.label}
    </span>
  )
}

export function VisibilityBadge({ visibilidade }: { visibilidade: Visibilidade }) {
  const meta = VISIBILIDADE_META[visibilidade]
  const Icon = meta.icon === 'globe' ? Globe : Lock
  return (
    <Badge tone={meta.tone}>
      <Icon size={12} />
      {meta.label}
    </Badge>
  )
}

export function StatusContaBadge({ status }: { status: StatusConta }) {
  const meta = STATUS_CONTA_META[status]
  return (
    <Badge tone={meta.tone} dot>
      {meta.label}
    </Badge>
  )
}

/** Estado da análise assíncrona de métricas de uma resolução. */
export function AnalysisStatus({ analisada, className }: { analisada: boolean; className?: string }) {
  if (analisada) {
    return (
      <span className={cn('inline-flex items-center gap-1.5 text-[11.5px] text-success', className)}>
        <Check size={12} />
        analisada
      </span>
    )
  }
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-md bg-info/10 px-2 py-0.5 font-mono text-[11.5px] font-semibold text-info ring-1 ring-inset ring-info/30',
        className,
      )}
    >
      <Spinner size={9} color="var(--info)" />
      calculando
    </span>
  )
}
