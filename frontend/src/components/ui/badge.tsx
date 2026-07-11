import type { LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

/**
 * Tons genéricos do sistema ÓRBITA. Não existe tom de marca — o colormap de
 * complexidade é a única fonte de cor, e ele vive nos chips de DOMÍNIO
 * (`components/domain/badges.tsx`), não aqui.
 */
export type Tom = 'neutro' | 'sucesso' | 'atencao' | 'erro' | 'info'

const tomBadge: Record<Tom, string> = {
  neutro: 'border-line-strong bg-recess text-mid',
  info: 'border-line-strong bg-recess text-ink',
  sucesso: 'border-sucesso-line bg-sucesso-bg text-sucesso-ink',
  atencao: 'border-atencao-line bg-atencao-bg text-atencao-ink',
  erro: 'border-erro-line bg-erro-bg text-erro-texto',
}

const tomMarcador: Record<Tom, string> = {
  neutro: 'bg-mid',
  info: 'bg-ink',
  sucesso: 'bg-sucesso',
  atencao: 'bg-atencao',
  erro: 'bg-erro-estrutura',
}

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  tom?: Tom
  /** Ponto circular 6px à esquerda, na cor do tom. */
  dot?: boolean
  icon?: LucideIcon
}

/**
 * Selo de estado: mono 11/600 MAIÚSCULO `.06em`, raio 3px, hairline.
 * Ex.: `PÚBLICO`, `AGUARDANDO VERIFICAÇÃO`, `CONTA ATIVA`.
 */
export function Badge({
  tom = 'neutro',
  dot,
  icon: Icon,
  className,
  children,
  ...props
}: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-[7px] rounded-ci border px-[10px] py-[4px]',
        'font-mono text-[11px] leading-none font-semibold tracking-[.06em] uppercase',
        tomBadge[tom],
        className,
      )}
      {...props}
    >
      {dot && (
        <span
          aria-hidden
          className={cn('size-[6px] shrink-0 rounded-full', tomMarcador[tom])}
        />
      )}
      {Icon && <Icon size={11} strokeWidth={2} aria-hidden className="shrink-0" />}
      {children}
    </span>
  )
}

export interface ChipProps extends React.HTMLAttributes<HTMLSpanElement> {
  tom?: Tom
  icon?: LucideIcon
  /** Ponto circular 7px à esquerda; use `cor` para pintá-lo fora do sistema de tons. */
  dot?: boolean
  /** Cor explícita do ponto (ex.: cor da linguagem). Sobrepõe o tom. */
  dotColor?: string
  /** Texto do chip em Space Grotesk em vez de mono. Padrão: mono (chip MEDE). */
  sans?: boolean
}

/**
 * Chip de dado: mono 12/500, caixa normal, fundo `recess` + hairline `line`.
 * Mais quieto que o `Badge` — carrega dado, não estado.
 */
export function Chip({
  tom = 'neutro',
  icon: Icon,
  dot,
  dotColor,
  sans,
  className,
  children,
  ...props
}: ChipProps) {
  const neutro = tom === 'neutro'
  return (
    <span
      className={cn(
        'inline-flex items-center gap-[7px] rounded-ci border px-[10px] py-[4px]',
        'text-[12px] leading-none font-medium',
        sans ? 'font-sans' : 'font-mono',
        neutro ? 'border-line bg-recess text-body' : tomBadge[tom],
        className,
      )}
      {...props}
    >
      {(dot || dotColor) && (
        <span
          aria-hidden
          className={cn('size-[7px] shrink-0 rounded-full', !dotColor && tomMarcador[tom])}
          style={dotColor ? { background: dotColor } : undefined}
        />
      )}
      {Icon && <Icon size={12} strokeWidth={2} aria-hidden className="shrink-0" />}
      {children}
    </span>
  )
}
