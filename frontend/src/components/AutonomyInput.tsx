import { useRef } from 'react'
import { cn } from '@/lib/utils'

const NIVEIS = [1, 2, 3, 4, 5] as const

export type NivelAutonomia = (typeof NIVEIS)[number]

export interface AutonomyInputProps {
  /** Índice de Autonomia IA autodeclarado. 1 = muito apoio de IA · 5 = autônomo. */
  value: NivelAutonomia | null
  onChange: (value: NivelAutonomia) => void
  disabled?: boolean
  /** `id` do primeiro botão — permite `<Label htmlFor>` apontar para o grupo. */
  id?: string
  /** `id` da mensagem de erro/hint do `FormField`. */
  'aria-describedby'?: string
  'aria-invalid'?: boolean
  className?: string
}

/**
 * Entrada 1–5 do Índice de Autonomia IA: 5 botões soltos (`flex:1; h38; gap:7`).
 * A variante "trilho contínuo" do guia de estilos NÃO vai ao produto (§5.14).
 *
 * ⚠ Cor NEUTRA (osso/tinta) — autonomia nunca usa o colormap (regra 4).
 *
 * TECLADO (WAI-ARIA radiogroup): UM único tab stop (roving tabindex) e as SETAS movem a
 * seleção — antes os 5 botões eram todos tabbable e as setas não faziam nada, o que é um
 * radiogroup só de fachada. Mesmo padrão do `SeletorDeGrafico`.
 */
export function AutonomyInput({
  value,
  onChange,
  disabled,
  id,
  'aria-describedby': describedBy,
  'aria-invalid': invalid,
  className,
}: AutonomyInputProps) {
  const refs = useRef<(HTMLButtonElement | null)[]>([])

  /** Índice que recebe o foco quando o grupo entra na tabulação: o selecionado, ou o 1º. */
  const indiceFocavel = value ? NIVEIS.indexOf(value) : 0

  function mover(de: number, passo: number) {
    const alvo = (de + passo + NIVEIS.length) % NIVEIS.length
    onChange(NIVEIS[alvo])
    refs.current[alvo]?.focus()
  }

  function aoTeclar(evento: React.KeyboardEvent<HTMLButtonElement>, indice: number) {
    switch (evento.key) {
      case 'ArrowRight':
      case 'ArrowDown':
        evento.preventDefault()
        mover(indice, 1)
        break
      case 'ArrowLeft':
      case 'ArrowUp':
        evento.preventDefault()
        mover(indice, -1)
        break
      case 'Home':
        evento.preventDefault()
        mover(0, 0)
        break
      case 'End':
        evento.preventDefault()
        mover(NIVEIS.length - 1, 0)
        break
      default:
        break
    }
  }

  return (
    <div
      role="radiogroup"
      aria-label="Índice de Autonomia IA"
      aria-describedby={describedBy}
      aria-invalid={invalid || undefined}
      className={cn('flex gap-[7px]', className)}
    >
      {NIVEIS.map((nivel, indice) => {
        const selecionado = nivel === value
        return (
          <button
            key={nivel}
            id={indice === 0 ? id : undefined}
            ref={(el) => {
              refs.current[indice] = el
            }}
            type="button"
            role="radio"
            aria-checked={selecionado}
            tabIndex={indice === indiceFocavel ? 0 : -1}
            disabled={disabled}
            onClick={() => onChange(nivel)}
            onKeyDown={(evento) => aoTeclar(evento, indice)}
            className={cn(
              'ci-foco-botao flex h-[38px] flex-1 cursor-pointer items-center justify-center rounded-ci border font-mono text-[14px] transition-colors',
              'disabled:cursor-not-allowed disabled:opacity-60',
              selecionado
                ? 'border-ink bg-ink font-semibold text-ink-on'
                : 'border-line-strong bg-panel text-soft hover:text-mid',
            )}
          >
            {nivel}
          </button>
        )
      })}
    </div>
  )
}
