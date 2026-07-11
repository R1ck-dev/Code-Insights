import type { LucideIcon } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogIconTile,
  DialogTitle,
} from './dialog'
import { Button } from './button'

/*
 * ConfirmDialog · ÓRBITA (04 §5.4 "Tornar público?" e §5.5 "Remover desafio?")
 * Caixa de 420px: padding 24 · gap 14 · tile de ícone 46×46 · h3 18/600 ·
 * corpo 13/1.55 · ações à direita (Cancelar + confirmação).
 * `destructive` → tile de erro + botão destrutivo sólido.
 */

export interface ConfirmDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  icon: LucideIcon
  title: string
  description: string
  confirmLabel: string
  cancelLabel?: string
  onConfirm: () => void
  loading?: boolean
  /** true → visual destrutivo (tile de erro + botão sólido `#CE4C55`). */
  destructive?: boolean
}

export function ConfirmDialog({
  open,
  onOpenChange,
  icon,
  title,
  description,
  confirmLabel,
  cancelLabel = 'Cancelar',
  onConfirm,
  loading,
  destructive,
}: ConfirmDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent width={420} className="gap-3.5 p-6">
        <DialogIconTile icon={icon} variant={destructive ? 'destructive' : 'neutral'} />
        <div className="flex flex-col gap-[7px]">
          <DialogTitle className="text-[18px]">{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </div>
        <div className="mt-1 flex justify-end gap-2.5">
          <Button variant="secondary" onClick={() => onOpenChange(false)} disabled={loading}>
            {cancelLabel}
          </Button>
          <Button
            variant={destructive ? 'destructive-solid' : 'primary'}
            onClick={onConfirm}
            loading={loading}
          >
            {confirmLabel}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
