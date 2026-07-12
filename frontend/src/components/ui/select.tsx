import * as SelectPrimitive from '@radix-ui/react-select'
import { Check, ChevronDown, ChevronUp, type LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

/*
 * Select · ÓRBITA — serve a dois papéis (00-INDICE §3.1):
 *  · variant="filtro" (padrão) = FilterPill das telas L, O e Explorar:
 *    h34 · `panel` + borda `line` · mono 12px + chevronDown 14px.
 *  · variant="campo" = select de formulário (diálogo "Novo snippet", 04 §7.4):
 *    h40 · `recess` + borda `line-strong` · mono 13px. `valid` → borda e anel verdes.
 */

export const Select = SelectPrimitive.Root
export const SelectValue = SelectPrimitive.Value
export const SelectGroup = SelectPrimitive.Group

export type SelectVariant = 'filtro' | 'campo'

export interface SelectTriggerProps
  extends React.ComponentProps<typeof SelectPrimitive.Trigger> {
  variant?: SelectVariant
  /** Ícone à esquerda do rótulo (ex.: ícone da categoria do snippet). */
  icon?: LucideIcon
  /** Filtro com valor aplicado → borda `line-strong` + texto `ink` (04 §4.2). */
  ativo?: boolean
  /** Campo preenchido/válido → borda `#4FB477` + anel verde a 22% (04 §7.4). */
  valid?: boolean
}

export function SelectTrigger({
  className,
  children,
  variant = 'filtro',
  icon: Icon,
  ativo,
  valid,
  style,
  ...props
}: SelectTriggerProps) {
  const campo = variant === 'campo'
  return (
    <SelectPrimitive.Trigger
      /*
       * O anel de "campo válido" sai por CLASSE (`ci-anel-valido` + `data-valido`), não por
       * `style` inline: inline vencia o `box-shadow` do `:focus-visible` e, depois de escolher
       * a categoria, tabular até o trigger não mostrava foco nenhum (WCAG 2.4.7).
       */
      data-valido={valid ? true : undefined}
      style={style}
      className={cn(
        'ci-foco-botao ci-anel-valido inline-flex cursor-pointer items-center rounded-ci border font-mono outline-none transition-colors',
        'disabled:cursor-not-allowed disabled:opacity-55',
        campo
          ? 'h-10 w-full justify-between gap-2 bg-recess px-3 text-[13px] text-ink data-[placeholder]:text-mid'
          : 'h-[34px] gap-[7px] bg-panel px-3 text-[12px]',
        campo
          ? valid
            ? 'border-sucesso'
            : 'border-line-strong'
          : ativo
            ? 'border-line-strong text-ink'
            : 'border-line text-mid hover:border-line-strong hover:text-ink',
        className,
      )}
      {...props}
    >
      {Icon && <Icon size={14} strokeWidth={2} aria-hidden className="shrink-0 text-steel" />}
      <span className="truncate">{children}</span>
      <SelectPrimitive.Icon asChild>
        <ChevronDown
          size={campo ? 15 : 14}
          strokeWidth={2}
          aria-hidden
          className="shrink-0 text-soft"
        />
      </SelectPrimitive.Icon>
    </SelectPrimitive.Trigger>
  )
}

export function SelectContent({
  className,
  children,
  position = 'popper',
  sideOffset = 5,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Content>) {
  return (
    <SelectPrimitive.Portal>
      <SelectPrimitive.Content
        position={position}
        sideOffset={sideOffset}
        className={cn(
          'z-50 max-h-[300px] min-w-[var(--radix-select-trigger-width)] overflow-hidden',
          'rounded-ci border border-line-strong bg-panel p-1 shadow-callout',
          className,
        )}
        {...props}
      >
        <SelectPrimitive.ScrollUpButton className="flex h-5 items-center justify-center text-soft">
          <ChevronUp size={13} strokeWidth={2} aria-hidden />
        </SelectPrimitive.ScrollUpButton>
        <SelectPrimitive.Viewport className="flex flex-col">{children}</SelectPrimitive.Viewport>
        <SelectPrimitive.ScrollDownButton className="flex h-5 items-center justify-center text-soft">
          <ChevronDown size={13} strokeWidth={2} aria-hidden />
        </SelectPrimitive.ScrollDownButton>
      </SelectPrimitive.Content>
    </SelectPrimitive.Portal>
  )
}

export function SelectItem({
  className,
  children,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Item>) {
  return (
    <SelectPrimitive.Item
      className={cn(
        'relative flex cursor-pointer select-none items-center gap-2 rounded-ci py-[7px] pl-2.5 pr-8',
        'font-mono text-[12.5px] text-mid outline-none transition-colors',
        'data-[highlighted]:bg-elevated data-[highlighted]:text-ink data-[state=checked]:text-ink',
        'data-[disabled]:pointer-events-none data-[disabled]:opacity-50',
        className,
      )}
      {...props}
    >
      <SelectPrimitive.ItemText>{children}</SelectPrimitive.ItemText>
      <SelectPrimitive.ItemIndicator className="absolute right-2.5 text-ink">
        <Check size={13} strokeWidth={2} aria-hidden />
      </SelectPrimitive.ItemIndicator>
    </SelectPrimitive.Item>
  )
}

/** Rótulo de grupo · mono 10.5px MAIÚSCULO. */
export function SelectLabel({
  className,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Label>) {
  return (
    <SelectPrimitive.Label
      className={cn(
        'px-2.5 py-1.5 font-mono text-[10.5px] uppercase tracking-[.08em] text-soft',
        className,
      )}
      {...props}
    />
  )
}

export function SelectSeparator({
  className,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Separator>) {
  return (
    <SelectPrimitive.Separator
      className={cn('my-1 h-px bg-line-soft', className)}
      {...props}
    />
  )
}
