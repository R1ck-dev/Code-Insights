import { cn } from '@/lib/utils'

/**
 * Marca ÓRBITA (spec 01 §8): elipse orbital inclinada -28° + núcleo + satélite.
 *
 * O núcleo é `#E08A3C` — que É, deliberadamente, a classe `O(n²)` do colormap.
 * É a única cor da marca porque é a única cor do sistema. Idêntico nos dois modos.
 * A elipse usa `--grid-soft` (#33415A escuro / #AEB8CC claro) e o satélite usa
 * `--badge` (#D6DEFF escuro / #1A2436 claro).
 */

/** Tamanhos canônicos: 24 = sidebar · 26 = nav pública/auth · 30 = masthead. */
export type LogoSize = 24 | 26 | 30

/** Wordmark por tamanho do símbolo (spec 01 §8 + spec 03 §nav pública). */
const TAMANHO_WORDMARK: Record<LogoSize, number> = { 24: 15, 26: 16, 30: 19 }
const GAP_LOCKUP: Record<LogoSize, number> = { 24: 9, 26: 10, 30: 12 }

interface LogoMarkProps {
  size?: LogoSize
  className?: string
}

/** Só o símbolo (sem wordmark). */
export function LogoMark({ size = 26, className }: LogoMarkProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 30 30"
      fill="none"
      className={cn('shrink-0', className)}
      aria-hidden="true"
      focusable="false"
    >
      <ellipse
        cx="15"
        cy="15"
        rx="13"
        ry="6.2"
        stroke="var(--grid-soft)"
        strokeWidth="1"
        transform="rotate(-28 15 15)"
      />
      {/* núcleo = O(n²) do colormap — mesmo hex nos dois modos, por contrato */}
      <circle cx="15" cy="15" r="3.4" fill="#E08A3C" />
      <circle cx="26" cy="9.5" r="1.6" fill="var(--badge)" />
    </svg>
  )
}

interface LogoProps {
  size?: LogoSize
  /** Mostra "CodeInsights" ao lado do símbolo. */
  wordmark?: boolean
  /**
   * Kicker "ÓRBITA · v3" (mono 10px, .12em, `soft`) sob o wordmark.
   * É um carimbo do guia de estilos — **não usar nas telas do produto**.
   */
  kicker?: boolean
  className?: string
}

/** Lockup da marca: símbolo + wordmark (+ kicker opcional). */
export function Logo({ size = 26, wordmark = true, kicker = false, className }: LogoProps) {
  if (!wordmark && !kicker) return <LogoMark size={size} className={className} />

  return (
    <span
      className={cn('inline-flex items-center', className)}
      style={{ gap: GAP_LOCKUP[size] }}
    >
      <LogoMark size={size} />
      <span className="flex flex-col gap-px">
        {wordmark && (
          <span
            className="font-sans font-bold leading-none text-ink"
            style={{ fontSize: TAMANHO_WORDMARK[size], letterSpacing: '-0.02em' }}
          >
            CodeInsights
          </span>
        )}
        {kicker && (
          <span
            className="font-mono leading-none text-soft"
            style={{ fontSize: 10, letterSpacing: '0.12em' }}
          >
            ÓRBITA · v3
          </span>
        )}
      </span>
    </span>
  )
}
