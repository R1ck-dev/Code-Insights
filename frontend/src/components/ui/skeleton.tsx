import { cn } from '@/lib/utils'

/**
 * Barra/bloco de esqueleto. Raio **2px** (exceção declarada ao raio 3px do
 * sistema) + `ciPulse 1.4s` — desligado sob `prefers-reduced-motion`.
 */
export function Skeleton({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div aria-hidden className={cn('ci-pulse rounded-ci-sm bg-elevated', className)} {...props} />
}

/** Larguras canônicas das barras do cartão de carregamento (01 §7.11). */
const LARGURAS = ['56%', '90%', '72%']

interface LoadingSkeletonProps {
  /** Quantidade de barras. Padrão 3 (56% · 90% · 72%). */
  linhas?: number
  /** Legenda mono sob as barras (ex.: `calculando…`). Opcional. */
  legenda?: string
  className?: string
}

/**
 * Cartão de carregamento (01 §7.11): recuo + hairline, 3 barras pulsando.
 * A primeira barra é `line` (mais forte), as demais `elevated`.
 */
export function LoadingSkeleton({ linhas = 3, legenda, className }: LoadingSkeletonProps) {
  return (
    <div
      role="status"
      aria-busy
      aria-label="Carregando"
      className={cn('flex flex-col gap-[11px] rounded-ci border border-line bg-recess p-[18px]', className)}
    >
      {Array.from({ length: linhas }, (_, i) => (
        <Skeleton
          key={i}
          className={cn('h-[11px]', i === 0 ? 'bg-line' : 'bg-elevated')}
          style={{ width: LARGURAS[i % LARGURAS.length] }}
        />
      ))}
      {legenda && <span className="mt-0.5 font-mono text-[11px] text-soft">{legenda}</span>}
    </div>
  )
}
