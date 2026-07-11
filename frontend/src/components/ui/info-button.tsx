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
 * Gatilho "?" que abre o diálogo explicando um conceito em seções (técnico +
 * leigo). Vive ao lado das métricas — no card e no gráfico da complexidade típica.
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
          'ci-foco-botao inline-flex h-5 w-5 shrink-0 cursor-pointer items-center justify-center rounded-ci text-soft transition-colors hover:bg-elevated hover:text-ink',
          className,
        )}
      >
        <HelpCircle size={size} strokeWidth={2} />
      </DialogTrigger>

      <DialogContent className="max-w-[520px] rounded-ci border-line-strong bg-panel shadow-modal">
        <DialogHeader className="border-line">
          <div className="flex flex-col gap-1 pr-6">
            <DialogTitle className="text-[18px] font-semibold text-ink">{titulo}</DialogTitle>
            {subtitulo && (
              <DialogDescription className="font-mono text-[11px] tracking-[.04em] text-soft">
                {subtitulo}
              </DialogDescription>
            )}
          </div>
        </DialogHeader>

        <DialogBody>
          {secoes.map((secao) => (
            <div key={secao.rotulo} className="flex flex-col gap-1.5">
              <span className="font-mono text-[11px] uppercase tracking-[.08em] text-mid">
                {secao.rotulo}
              </span>
              <p className="text-[13px] leading-[1.55] text-body">{secao.texto}</p>
            </div>
          ))}
        </DialogBody>
      </DialogContent>
    </Dialog>
  )
}
