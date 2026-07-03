import { useTheme } from '@/theme/ThemeProvider'
import { cn } from '@/lib/utils'

type MeterSize = 'sm' | 'md' | 'lg'

// [larguraSeg, alturaSeg, gap, fonteLabel]
const DIMS: Record<MeterSize, [number, number, number, number]> = {
  sm: [14, 6, 3, 12],
  md: [22, 9, 4, 13],
  lg: [30, 11, 5, 15],
}

interface AutonomyMeterProps {
  /** Índice de Autonomia IA, 0–5. */
  value: number
  size?: MeterSize
  showLabel?: boolean
  className?: string
}

/**
 * Medidor de 5 segmentos do Índice de Autonomia IA (1–5, autodeclarado).
 * Tom neutro/índigo — é variável de pesquisa, não julgamento de bom/ruim.
 */
export function AutonomyMeter({ value, size = 'md', showLabel = true, className }: AutonomyMeterProps) {
  const { theme } = useTheme()
  const v = Math.max(0, Math.min(5, Math.round(value)))
  const [w, h, gap, fs] = DIMS[size]

  const on = '#6E5FF6'
  const onBorder = '#8B85FF'
  const off = theme === 'dark' ? '#23252E' : '#EDEEF2'
  const offBorder = theme === 'dark' ? '#2C2F3A' : '#DEE0E6'

  return (
    <div className={cn('inline-flex items-center gap-2.5', className)}>
      <div className="flex items-center" style={{ gap }}>
        {[1, 2, 3, 4, 5].map((i) => (
          <span
            key={i}
            style={{
              width: w,
              height: h,
              borderRadius: 3,
              background: i <= v ? on : off,
              boxShadow: `inset 0 0 0 1px ${i <= v ? onBorder : offBorder}`,
            }}
          />
        ))}
      </div>
      {showLabel && (
        <span
          className="font-mono font-semibold text-heading tabular-nums"
          style={{ fontSize: fs, letterSpacing: '0.01em' }}
        >
          {v}/5
        </span>
      )}
    </div>
  )
}
