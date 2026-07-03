import { cn } from '@/lib/utils'
import { formatDate } from '@/lib/utils'

interface MetricCardProps {
  nome: string
  sub?: string
  rotulo: string
  natureza: 'exata' | 'estimada'
  /** cor do valor (hex da escala p/ estimadas; default heading). */
  valueColor?: string
  detalhe?: string | null
  analisadoEm?: string | null
  className?: string
}

/** Cartão de uma métrica (ciclomática/tempo/espaço) com chip exata/estimada. */
export function MetricCard({
  nome,
  sub,
  rotulo,
  natureza,
  valueColor,
  detalhe,
  analisadoEm,
  className,
}: MetricCardProps) {
  const exata = natureza === 'exata'
  return (
    <div
      className={cn(
        'flex min-w-[200px] flex-col gap-[11px] rounded-xl border border-border bg-surface p-4',
        className,
      )}
    >
      <div className="flex items-start justify-between gap-2.5">
        <div className="flex flex-col gap-0.5">
          <span className="text-[12.5px] font-semibold text-muted">{nome}</span>
          {sub && <span className="font-mono text-[10.5px] tracking-wide text-subtle">{sub}</span>}
        </div>
        <span
          className={cn(
            'shrink-0 rounded-full px-2 py-[3px] font-mono text-[10.5px] font-semibold uppercase tracking-wider ring-1 ring-inset',
            exata
              ? 'text-success bg-success/10 ring-success/30'
              : 'text-warning bg-warning/[.12] ring-warning/30',
          )}
        >
          {natureza}
        </span>
      </div>
      <div
        className="font-mono text-[34px] font-semibold leading-none tabular-nums"
        style={{ color: valueColor ?? 'var(--heading)', letterSpacing: '-0.01em' }}
      >
        {rotulo}
      </div>
      {detalhe && (
        <p className="line-clamp-3 text-[12.5px] leading-relaxed text-subtle">{detalhe}</p>
      )}
      {analisadoEm && (
        <span className="mt-px font-mono text-[10.5px] text-subtle">
          analisado em {formatDate(analisadoEm)}
        </span>
      )}
    </div>
  )
}
