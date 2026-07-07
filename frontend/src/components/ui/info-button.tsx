import { HelpCircle } from 'lucide-react'
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { cn } from '@/lib/utils'
import type { InfoSecao } from '@/domain/metricas-explicacao'

interface InfoButtonProps {
  titulo: string
  subtitulo?: string
  secoes: InfoSecao[]
  /** Rótulo acessível do gatilho (ex.: "O que é complexidade ciclomática?"). */
  ariaLabel: string
  className?: string
  size?: number
}

/**
 * Gatilho "?" que abre um diálogo explicando um conceito em seções (técnico +
 * leigo). Reutilizado no retrato de métricas e nos cards do dashboard.
 */
export function InfoButton({
  titulo,
  subtitulo,
  secoes,
  ariaLabel,
  className,
  size = 15,
}: InfoButtonProps) {
  return (
    <Dialog>
      <DialogTrigger
        aria-label={ariaLabel}
        className={cn(
          'inline-flex h-5 w-5 shrink-0 cursor-pointer items-center justify-center rounded-full text-subtle transition-colors hover:bg-surface-2 hover:text-fg focus:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
          className,
        )}
      >
        <HelpCircle size={size} />
      </DialogTrigger>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <div className="flex flex-col gap-0.5 pr-6">
            <DialogTitle>{titulo}</DialogTitle>
            {subtitulo && <DialogDescription>{subtitulo}</DialogDescription>}
          </div>
        </DialogHeader>
        <DialogBody>
          {secoes.map((s) => (
            <div key={s.rotulo} className="flex flex-col gap-1">
              <span className="text-[11px] font-semibold uppercase tracking-wide text-subtle">
                {s.rotulo}
              </span>
              <p className="text-[13.5px] leading-relaxed text-fg">{s.texto}</p>
            </div>
          ))}
        </DialogBody>
      </DialogContent>
    </Dialog>
  )
}
