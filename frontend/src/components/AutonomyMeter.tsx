import { cn } from '@/lib/utils'

export type AutonomyMeterSize = 'sm' | 'md' | 'lg'

/** [larguraSeg, alturaSeg, gap, fonteLabel] — spec 05 §2.2. */
const DIMS: Record<AutonomyMeterSize, [number, number, number, number]> = {
  sm: [14, 6, 3, 12],
  md: [22, 9, 4, 13],
  lg: [30, 11, 5, 15],
}

export interface AutonomyMeterProps {
  /** Índice de Autonomia IA, 0–5 (clamp). 1 = muito apoio de IA · 5 = autônomo. */
  value: number
  size?: AutonomyMeterSize
  showLabel?: boolean
  className?: string
}

/**
 * Medidor de 5 segmentos do Índice de Autonomia IA (autodeclarado).
 *
 * ⚠ REGRA 4 do sistema: a cor é NEUTRA (osso no escuro / tinta no claro) e
 * NUNCA usa o colormap de complexidade — no Órbita, autonomia é "sinal branco".
 * Cores vêm dos tokens `--autonomia-on/off/off-line` (§2.1 · #EAEEF6 / #1B2433 /
 * #2A3547 no escuro; #1A2436 / #E1E6F0 / #CDD5E4 no claro).
 */
export function AutonomyMeter({
  value,
  size = 'md',
  showLabel = true,
  className,
}: AutonomyMeterProps) {
  const v = Number.isFinite(value) ? Math.max(0, Math.min(5, Math.round(value))) : 0
  const [w, h, gap, fs] = DIMS[size]

  return (
    <div
      className={cn('inline-flex items-center gap-2.5', className)}
      role="img"
      aria-label={`Índice de Autonomia IA: ${v} de 5`}
    >
      <div className="flex items-center" style={{ gap }} aria-hidden>
        {[1, 2, 3, 4, 5].map((i) => {
          const cheio = i <= v
          return (
            <span
              key={i}
              style={{
                width: w,
                height: h,
                borderRadius: 3,
                background: cheio ? 'var(--autonomia-on)' : 'var(--autonomia-off)',
                // A borda é inset shadow (não `border`) — não afeta o box model.
                boxShadow: `inset 0 0 0 1px ${
                  cheio ? 'var(--autonomia-on)' : 'var(--autonomia-off-line)'
                }`,
              }}
            />
          )
        })}
      </div>
      {showLabel && (
        <span
          aria-hidden
          className="font-mono font-semibold tabular-nums"
          style={{ fontSize: fs, letterSpacing: '0.01em', color: 'var(--autonomia-on)' }}
        >
          {v}/5
        </span>
      )}
    </div>
  )
}
