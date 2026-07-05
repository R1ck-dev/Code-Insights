import type { LucideIcon } from 'lucide-react'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './dialog'
import { Button, type ButtonVariant } from './button'
import { cn } from '@/lib/utils'

interface ConfirmDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  icon: LucideIcon
  title: string
  description: string
  confirmLabel: string
  cancelLabel?: string
  onConfirm: () => void
  loading?: boolean
  /** true → visual destrutivo (vermelho). */
  destructive?: boolean
}

/** Diálogo de confirmação (tornar público, remover…), no padrão do design. */
export function ConfirmDialog({
  open,
  onOpenChange,
  icon: Icon,
  title,
  description,
  confirmLabel,
  cancelLabel = 'Cancelar',
  onConfirm,
  loading,
  destructive,
}: ConfirmDialogProps) {
  const confirmVariant: ButtonVariant = destructive ? 'destructiveSolid' : 'primary'
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md" showClose={false}>
        <div className="flex flex-col gap-3.5 p-6">
          <div
            className={cn(
              'flex h-[46px] w-[46px] items-center justify-center rounded-[13px]',
              destructive ? 'bg-danger/[.12]' : 'bg-brand/[.12]',
            )}
          >
            <Icon size={22} className={destructive ? 'text-danger' : 'text-brand-strong'} />
          </div>
          <div className="flex flex-col gap-1.5">
            <DialogTitle className="text-[18px] font-bold text-heading">{title}</DialogTitle>
            <DialogDescription className="text-[13.5px] leading-relaxed text-muted">
              {description}
            </DialogDescription>
          </div>
          <div className="mt-1 flex justify-end gap-2.5">
            <Button variant="secondary" onClick={() => onOpenChange(false)} disabled={loading}>
              {cancelLabel}
            </Button>
            <Button variant={confirmVariant} onClick={onConfirm} loading={loading}>
              {confirmLabel}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
