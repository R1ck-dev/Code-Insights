import { cn } from '@/lib/utils'

type LogoSize = 'sm' | 'md' | 'lg'

const dims: Record<LogoSize, { box: number; barW: number; heights: [number, number, number]; gap: number; radius: number; font: number }> = {
  sm: { box: 26, barW: 2.5, heights: [5, 8, 11], gap: 2, radius: 7, font: 15 },
  md: { box: 28, barW: 3, heights: [6, 9, 12], gap: 2.5, radius: 8, font: 16 },
  lg: { box: 32, barW: 3.5, heights: [7, 11, 15], gap: 3, radius: 9, font: 18 },
}

/** Marca de barras (gráfico ascendente) em gradiente índigo. */
export function LogoMark({ size = 'md', className }: { size?: LogoSize; className?: string }) {
  const d = dims[size]
  return (
    <div
      className={cn('flex shrink-0 items-end justify-center', className)}
      style={{
        width: d.box,
        height: d.box,
        borderRadius: d.radius,
        gap: d.gap,
        padding: d.box * 0.23,
        background: 'linear-gradient(150deg,#8B85FF,#6E5FF6 55%,#5B4FD8)',
      }}
      aria-hidden
    >
      {d.heights.map((h, i) => (
        <span
          key={i}
          style={{
            width: d.barW,
            height: h,
            borderRadius: 2,
            background: i === 2 ? '#fff' : `rgba(255,255,255,${0.85 + i * 0.05})`,
          }}
        />
      ))}
    </div>
  )
}

export function Logo({
  size = 'md',
  wordmark = true,
  className,
}: {
  size?: LogoSize
  wordmark?: boolean
  className?: string
}) {
  const d = dims[size]
  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <LogoMark size={size} />
      {wordmark && (
        <span
          className="font-bold tracking-tight text-heading"
          style={{ fontSize: d.font, letterSpacing: '-0.02em' }}
        >
          Code<span className="text-brand-strong">Insights</span>
        </span>
      )}
    </span>
  )
}
