import { cn } from '@/lib/utils'
import type { Tone } from '@/domain/enums'

const toneClasses: Record<Tone, string> = {
  brand: 'text-brand-strong bg-brand/[.13] ring-brand/30',
  success: 'text-success bg-success/10 ring-success/30',
  warning: 'text-warning bg-warning/[.12] ring-warning/30',
  danger: 'text-danger bg-danger/10 ring-danger/30',
  info: 'text-info bg-info/10 ring-info/30',
  neutral: 'text-neutral bg-neutral/10 ring-neutral/25',
}

const dotColor: Record<Tone, string> = {
  brand: 'bg-brand-strong',
  success: 'bg-success',
  warning: 'bg-warning',
  danger: 'bg-danger',
  info: 'bg-info',
  neutral: 'bg-neutral',
}

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  tone?: Tone
  dot?: boolean
}

/** Pílula de status com cor semântica (Público, Ativa, exata…). */
export function Badge({ tone = 'neutral', dot, className, children, ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11.5px] font-semibold ring-1 ring-inset',
        toneClasses[tone],
        className,
      )}
      {...props}
    >
      {dot && <span className={cn('h-1.5 w-1.5 rounded-full', dotColor[tone])} />}
      {children}
    </span>
  )
}

/** Chip neutro contornado (plataforma, identificador externo). */
export function Chip({
  className,
  mono,
  children,
  ...props
}: React.HTMLAttributes<HTMLSpanElement> & { mono?: boolean }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-md border border-border bg-input px-2 py-[3px] text-[11.5px] font-semibold text-label',
        mono && 'font-mono font-normal text-muted',
        className,
      )}
      {...props}
    >
      {children}
    </span>
  )
}
