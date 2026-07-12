import * as DropdownMenuPrimitive from '@radix-ui/react-dropdown-menu'
import { cn } from '@/lib/utils'

/*
 * DropdownMenu · ÓRBITA — menu do usuário (topbar) e nav mobile.
 * Painel `panel` + hairline `line-strong` + raio 3px + `shadow-callout`.
 * Itens em mono 12.5/500 (mono mede: nav e rótulos de instrumento).
 */

export const DropdownMenu = DropdownMenuPrimitive.Root
export const DropdownMenuTrigger = DropdownMenuPrimitive.Trigger
export const DropdownMenuPortal = DropdownMenuPrimitive.Portal

export function DropdownMenuContent({
  className,
  align = 'end',
  sideOffset = 6,
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Content>) {
  return (
    <DropdownMenuPrimitive.Portal>
      <DropdownMenuPrimitive.Content
        align={align}
        sideOffset={sideOffset}
        className={cn(
          'z-50 min-w-[184px] rounded-ci border border-line-strong bg-panel p-1 shadow-callout',
          className,
        )}
        {...props}
      />
    </DropdownMenuPrimitive.Portal>
  )
}

export interface DropdownMenuItemProps
  extends React.ComponentProps<typeof DropdownMenuPrimitive.Item> {
  /** `danger` → texto `erro-texto` + realce `erro-bg` (equivale a `data-variant="danger"`). */
  variant?: 'default' | 'danger'
  inset?: boolean
}

export function DropdownMenuItem({
  className,
  inset,
  variant,
  ...props
}: DropdownMenuItemProps) {
  return (
    <DropdownMenuPrimitive.Item
      data-variant={variant}
      className={cn(
        'flex cursor-pointer select-none items-center gap-2.5 rounded-ci px-2.5 py-[7px]',
        'font-mono text-[12.5px] font-medium text-mid outline-none transition-colors',
        'data-[highlighted]:bg-elevated data-[highlighted]:text-ink',
        'data-[disabled]:pointer-events-none data-[disabled]:opacity-50',
        'data-[variant=danger]:text-erro-texto data-[variant=danger]:data-[highlighted]:bg-erro-bg data-[variant=danger]:data-[highlighted]:text-erro-texto',
        inset && 'pl-8',
        className,
      )}
      {...props}
    />
  )
}

export function DropdownMenuSeparator({
  className,
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Separator>) {
  return (
    <DropdownMenuPrimitive.Separator
      className={cn('my-1 h-px bg-line-soft', className)}
      {...props}
    />
  )
}

/** Rótulo de seção · mono 10.5px MAIÚSCULO `.08em`. */
export function DropdownMenuLabel({
  className,
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Label>) {
  return (
    <DropdownMenuPrimitive.Label
      className={cn(
        'px-2.5 py-1.5 font-mono text-[10.5px] uppercase tracking-[.08em] text-soft',
        className,
      )}
      {...props}
    />
  )
}
