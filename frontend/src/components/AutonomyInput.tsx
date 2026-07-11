import { cn } from '@/lib/utils'

const NIVEIS = [1, 2, 3, 4, 5] as const

export type NivelAutonomia = (typeof NIVEIS)[number]

export interface AutonomyInputProps {
  /** Índice de Autonomia IA autodeclarado. 1 = muito apoio de IA · 5 = autônomo. */
  value: NivelAutonomia | null
  onChange: (value: NivelAutonomia) => void
  disabled?: boolean
  className?: string
}

/**
 * Entrada 1–5 do Índice de Autonomia IA: 5 botões soltos (`flex:1; h38; gap:7`).
 * A variante "trilho contínuo" do guia de estilos NÃO vai ao produto (§5.14).
 *
 * ⚠ Cor NEUTRA (osso/tinta) — autonomia nunca usa o colormap (regra 4).
 */
export function AutonomyInput({ value, onChange, disabled, className }: AutonomyInputProps) {
  return (
    <div
      role="radiogroup"
      aria-label="Índice de Autonomia IA"
      className={cn('flex gap-[7px]', className)}
    >
      {NIVEIS.map((nivel) => {
        const selecionado = nivel === value
        return (
          <button
            key={nivel}
            type="button"
            role="radio"
            aria-checked={selecionado}
            disabled={disabled}
            onClick={() => onChange(nivel)}
            className={cn(
              'flex h-[38px] flex-1 items-center justify-center rounded-ci border font-mono text-[14px] transition-colors',
              'focus-visible:outline-none focus-visible:shadow-anel-botao',
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
