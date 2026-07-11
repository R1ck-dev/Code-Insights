import * as DialogPrimitive from '@radix-ui/react-dialog'
import { X, type LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

/*
 * Diálogo · ÓRBITA
 * Overlay rgba(6,7,13,.72) + blur(2px) · painel `panel` + hairline `line-strong`
 * + raio 3px + sombra `shadow-modal`. Larguras padronizadas: 560 / 520 / 420.
 * Fecha por Esc, clique no overlay e botão X (00-INDICE §6 Lacuna 14 — decidido:
 * o X existe, apesar de não estar no protótipo).
 */

export const Dialog = DialogPrimitive.Root
export const DialogTrigger = DialogPrimitive.Trigger
export const DialogClose = DialogPrimitive.Close
export const DialogPortal = DialogPrimitive.Portal

/** Larguras canônicas (00-INDICE §3.1): 560 = formulário largo · 520 = formulário · 420 = confirmação. */
export type DialogWidth = 560 | 520 | 420

export interface DialogContentProps
  extends React.ComponentProps<typeof DialogPrimitive.Content> {
  /** Botão `X` no canto superior direito. */
  showClose?: boolean
  /** Largura-alvo em px; sempre limitada a `calc(100vw - 32px)`. */
  width?: DialogWidth | number
}

export function DialogContent({
  className,
  children,
  showClose = true,
  width = 520,
  style,
  ...props
}: DialogContentProps) {
  return (
    <DialogPrimitive.Portal>
      <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-[rgba(26,36,54,0.42)] backdrop-blur-[2px] dark:bg-[rgba(6,7,13,0.72)]" />
      <DialogPrimitive.Content
        style={{ width: `min(${width}px, calc(100vw - 32px))`, ...style }}
        className={cn(
          'fixed left-1/2 top-1/2 z-50 flex max-h-[calc(100vh-48px)] -translate-x-1/2 -translate-y-1/2 flex-col',
          'overflow-hidden rounded-ci border border-line-strong bg-panel shadow-modal outline-none',
          className,
        )}
        {...props}
      >
        {children}
        {showClose && (
          <DialogPrimitive.Close
            aria-label="Fechar"
            className="ci-foco-botao absolute right-[14px] top-[14px] flex h-7 w-7 cursor-pointer items-center justify-center rounded-ci text-soft transition-colors hover:bg-elevated hover:text-ink"
          >
            <X size={16} strokeWidth={2} />
          </DialogPrimitive.Close>
        )}
      </DialogPrimitive.Content>
    </DialogPrimitive.Portal>
  )
}

export interface DialogHeaderProps extends React.HTMLAttributes<HTMLDivElement> {
  /** Ícone decorativo do cabeçalho (`plus` em "Novo desafio", `braces` em "Novo snippet"). */
  icon?: LucideIcon
}

/** Cabeçalho do chassi de formulário: padding 18/20, hairline embaixo, espaço reservado ao `X`. */
export function DialogHeader({ className, icon: Icon, children, ...props }: DialogHeaderProps) {
  return (
    <div
      className={cn(
        'flex shrink-0 items-start gap-2.5 border-b border-line py-[18px] pl-5 pr-[52px]',
        className,
      )}
      {...props}
    >
      {Icon && <Icon size={18} strokeWidth={2} className="mt-px shrink-0 text-soft" />}
      <div className="flex min-w-0 flex-col gap-1.5">{children}</div>
    </div>
  )
}

/** `h3` · Space Grotesk 17/600 (a confirmação usa 18 — passar `className="text-[18px]"`). */
export function DialogTitle({
  className,
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Title>) {
  return (
    <DialogPrimitive.Title
      className={cn('m-0 text-[17px] font-semibold leading-snug text-ink', className)}
      {...props}
    />
  )
}

/** Corpo do diálogo · Space Grotesk 13/1.55 `mid`. */
export function DialogDescription({
  className,
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Description>) {
  return (
    <DialogPrimitive.Description
      className={cn('text-[13px] leading-[1.55] text-mid', className)}
      {...props}
    />
  )
}

/** Corpo do chassi de formulário: padding 20, gap 15, rolagem própria. */
export function DialogBody({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('flex min-h-0 flex-1 flex-col gap-[15px] overflow-y-auto p-5', className)}
      {...props}
    />
  )
}

/** Rodapé do chassi de formulário: hairline em cima, ações à direita (gap 10). */
export function DialogFooter({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        'flex shrink-0 items-center justify-end gap-2.5 border-t border-line px-5 py-4',
        className,
      )}
      {...props}
    />
  )
}

export interface DialogIconTileProps extends React.HTMLAttributes<HTMLDivElement> {
  icon: LucideIcon
  /** `destructive` → tile de erro (fundo .10 / borda .34 / ícone `erro-texto`). */
  variant?: 'neutral' | 'destructive'
}

/** Tile 46×46 do cabeçalho dos diálogos de confirmação (01 §7.8). */
export function DialogIconTile({
  icon: Icon,
  variant = 'neutral',
  className,
  ...props
}: DialogIconTileProps) {
  const destrutivo = variant === 'destructive'
  return (
    <div
      className={cn(
        'flex h-[46px] w-[46px] shrink-0 items-center justify-center rounded-ci border',
        destrutivo
          ? 'border-erro-tile-line bg-erro-tile-bg'
          : 'border-line-strong bg-elevated',
        className,
      )}
      {...props}
    >
      <Icon
        size={destrutivo ? 21 : 22}
        strokeWidth={2}
        className={destrutivo ? 'text-erro-texto' : 'text-steel'}
      />
    </div>
  )
}
