import { forwardRef } from 'react'
import { cn } from '@/lib/utils'
import { Spinner } from './spinner'

export type ButtonVariant =
  | 'primary'
  | 'secondary'
  | 'ghost'
  | 'destructive'
  | 'destructiveSolid'
export type ButtonSize = 'sm' | 'md' | 'lg' | 'icon'

const base =
  'inline-flex items-center justify-center gap-2 rounded-[10px] font-semibold whitespace-nowrap transition-colors cursor-pointer disabled:pointer-events-none disabled:opacity-55 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/60'

const variants: Record<ButtonVariant, string> = {
  primary:
    'bg-brand text-white shadow-[0_6px_16px_-6px_rgba(110,95,246,.7)] hover:bg-brand-hover active:bg-brand-pressed',
  secondary:
    'border border-border-strong bg-transparent text-fg hover:bg-surface-2',
  ghost: 'bg-transparent text-muted hover:text-fg hover:bg-surface-2',
  destructive:
    'border border-danger/35 bg-danger/[.08] text-danger hover:bg-danger/[.14]',
  destructiveSolid: 'bg-danger text-white hover:brightness-110',
}

const sizes: Record<ButtonSize, string> = {
  sm: 'h-9 px-3 text-[13px]',
  md: 'h-10 px-4 text-sm',
  lg: 'h-[46px] px-5 text-[15px]',
  icon: 'h-[38px] w-[38px] p-0',
}

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  loading?: boolean
}

/** Classes de botão reutilizáveis (para aplicar em <Link> etc.). */
export function buttonClasses(opts?: {
  variant?: ButtonVariant
  size?: ButtonSize
  className?: string
}): string {
  return cn(
    base,
    variants[opts?.variant ?? 'primary'],
    sizes[opts?.size ?? 'md'],
    opts?.className,
  )
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = 'primary', size = 'md', loading, disabled, children, className, ...props },
  ref,
) {
  return (
    <button
      ref={ref}
      className={buttonClasses({ variant, size, className })}
      disabled={disabled || loading}
      {...props}
    >
      {loading && <Spinner size={16} color="currentColor" />}
      {children}
    </button>
  )
})
