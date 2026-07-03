import { cn } from '@/lib/utils'

interface SpinnerProps {
  size?: number
  className?: string
  /** cor do arco (default: info/azul, como no design de "calculando"). */
  color?: string
}

export function Spinner({ size = 18, className, color = 'var(--info)' }: SpinnerProps) {
  return (
    <span
      className={cn('ci-spin inline-block rounded-full', className)}
      style={{
        width: size,
        height: size,
        border: `2px solid color-mix(in srgb, ${color} 30%, transparent)`,
        borderTopColor: color,
      }}
      role="status"
      aria-label="Carregando"
    />
  )
}

export function FullScreenLoader() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-bg">
      <Spinner size={28} color="var(--brand)" />
    </div>
  )
}
