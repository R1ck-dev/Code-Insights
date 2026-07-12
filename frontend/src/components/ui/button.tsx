import { forwardRef } from 'react'
import type { LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

export type ButtonVariant =
  | 'primary'
  | 'secondary'
  | 'ghost'
  | 'destructive'
  | 'destructive-solid'
export type ButtonSize = 'sm' | 'md' | 'lg'
/** Mono na área autenticada · Space Grotesk nas telas de acesso/landing. */
export type ButtonFont = 'mono' | 'sans'

const base =
  'ci-foco-botao inline-flex cursor-pointer items-center justify-center gap-2 rounded-ci border ' +
  'whitespace-nowrap select-none transition-[background-color,border-color,color,filter] duration-100 ' +
  'disabled:cursor-not-allowed disabled:opacity-55'

const variantes: Record<ButtonVariant, string> = {
  primary: 'border-transparent bg-ink text-ink-on hover:bg-body',
  secondary: 'border-line-strong bg-transparent text-ink hover:bg-elevated',
  ghost: 'border-transparent bg-transparent text-mid hover:bg-elevated hover:text-ink',
  destructive:
    'border-erro-line bg-erro-bg text-erro-texto hover:bg-[rgba(var(--erro-rgb),0.14)]',
  // Rótulo osso fixo: `ink` inverteria no claro e ficaria ilegível sobre o vermelho. Os dois
  // tons SÃO tokens (`ink-on` no claro = #FBFCFE · `ink` no escuro = #EDF0FA): mesmo resultado,
  // zero hex. O escape `dark:` continua sendo necessário — é ele que trava a inversão.
  'destructive-solid':
    'border-transparent bg-erro-estrutura text-ink-on dark:text-ink hover:brightness-110',
}

const alturas: Record<ButtonSize, string> = {
  sm: 'h-[36px]',
  md: 'h-[40px]',
  lg: 'h-[44px]',
}

const paddings: Record<ButtonSize, string> = {
  sm: 'px-[13px]',
  md: 'px-[15px]',
  lg: 'px-[18px]',
}

const quadrados: Record<ButtonSize, string> = {
  sm: 'w-[36px] px-0',
  md: 'w-[40px] px-0',
  lg: 'w-[44px] px-0',
}

const fontes: Record<ButtonFont, Record<ButtonSize, string>> = {
  mono: {
    sm: 'font-mono text-[12.5px]',
    md: 'font-mono text-[12.5px]',
    lg: 'font-mono text-[13px]',
  },
  sans: {
    sm: 'font-sans text-[14px]',
    md: 'font-sans text-[14px]',
    lg: 'font-sans text-[13.5px]',
  },
}

const TAMANHO_ICONE: Record<ButtonSize, number> = { sm: 14, md: 15, lg: 16 }

export interface ButtonOptions {
  variant?: ButtonVariant
  size?: ButtonSize
  font?: ButtonFont
  fullWidth?: boolean
  /** Botão quadrado (só ícone): 36/40/44 px. */
  iconOnly?: boolean
  className?: string
}

/**
 * Classes de botão reutilizáveis — usadas por dezenas de `<Link>` que precisam
 * parecer botão. Mantenha este export.
 */
export function buttonClasses({
  variant = 'primary',
  size = 'md',
  font = 'mono',
  fullWidth,
  iconOnly,
  className,
}: ButtonOptions = {}): string {
  const forte = variant === 'primary' || variant === 'destructive-solid'
  return cn(
    base,
    variantes[variant],
    alturas[size],
    iconOnly ? quadrados[size] : paddings[size],
    fontes[font][size],
    forte ? 'font-semibold' : 'font-medium',
    fullWidth && 'w-full',
    className,
  )
}

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    ButtonOptions {
  icon?: LucideIcon
  /** Spinner `ciSpin` + `cursor:progress` + `disabled`. */
  loading?: boolean
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  {
    variant = 'primary',
    size = 'md',
    font = 'mono',
    fullWidth,
    iconOnly,
    icon: Icon,
    loading,
    disabled,
    className,
    children,
    ...props
  },
  ref,
) {
  return (
    <button
      ref={ref}
      type={props.type ?? 'button'}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cn(
        buttonClasses({ variant, size, font, fullWidth, iconOnly, className }),
        loading && 'cursor-progress',
      )}
      {...props}
    >
      {loading ? (
        <Spinner />
      ) : (
        Icon && <Icon size={TAMANHO_ICONE[size]} strokeWidth={2} aria-hidden />
      )}
      {children}
    </button>
  )
})

/** Anel `currentColor` a 28% com o topo cheio (§01 §7.1). */
function Spinner() {
  return (
    <span
      aria-hidden
      className="ci-spin shrink-0 rounded-full"
      style={{
        width: 13,
        height: 13,
        border: '2px solid color-mix(in srgb, currentColor 28%, transparent)',
        borderTopColor: 'currentColor',
      }}
    />
  )
}
