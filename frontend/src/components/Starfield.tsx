import type { CSSProperties } from 'react'
import { cn } from '@/lib/utils'

/**
 * Campo de estrelas (spec 01 §6.3 e §6.4). Duas camadas:
 *   (a) campo estático — `background-image` com N radial-gradients (classes do index.css);
 *   (b) estrelas que CINTILAM — spans com `ciTwinkle` (3,2–4,8s, delays variados).
 *
 * É CENÁRIO: `aria-hidden` + `pointer-events:none`. O pai precisa ser `relative`.
 * No modo claro as estrelas viram pontos de tinta e a cintilação é desligada
 * (`html:not(.dark) .ci-estrela`); sob `prefers-reduced-motion` também — ambos no index.css.
 */

/** Densidade do campo → classe do index.css (28 · 10 · 5 gradientes). */
export type StarfieldDensity = 'densa' | 'media' | 'esparsa'

const CLASSE_CAMPO: Record<StarfieldDensity, string> = {
  densa: 'ci-starfield',
  media: 'ci-starfield-auth',
  esparsa: 'ci-starfield-esparso',
}

/** Quantas das 10 estrelas cintilantes entram em cada densidade. */
const CINTILANTES_POR_DENSIDADE: Record<StarfieldDensity, number> = {
  densa: 10,
  media: 6,
  esparsa: 3,
}

interface EstrelaCintilante {
  top: string
  left: string
  lado: number
  duracao: string
  atraso?: string
  /** `badge` #D6DEFF (escuro) em vez de branco. */
  azul?: boolean
  /** Única exceção de glow do sistema — e ainda assim é cenário, não componente. */
  glow?: boolean
}

/** Tabela literal da spec 01 §6.4. */
const ESTRELAS: EstrelaCintilante[] = [
  { top: '9%', left: '26%', lado: 2, duracao: '3.2s' },
  { top: '16%', left: '71%', lado: 2.5, duracao: '4.1s', atraso: '.6s', azul: true },
  { top: '63%', left: '9%', lado: 2, duracao: '3.7s', atraso: '1.1s' },
  { top: '78%', left: '84%', lado: 2, duracao: '4.6s', atraso: '.3s' },
  { top: '28%', left: '12%', lado: 2, duracao: '3.9s', atraso: '.2s' },
  { top: '44%', left: '58%', lado: 2.6, duracao: '4.3s', atraso: '.8s', azul: true, glow: true },
  { top: '70%', left: '33%', lado: 2, duracao: '3.4s', atraso: '1.4s' },
  { top: '12%', left: '90%', lado: 2, duracao: '4.8s', atraso: '.5s' },
  { top: '86%', left: '20%', lado: 2.6, duracao: '3.6s', atraso: '1s', glow: true },
  { top: '54%', left: '77%', lado: 2, duracao: '4.1s', atraso: '.2s' },
]

interface StarfieldProps {
  density?: StarfieldDensity
  /** Desliga a camada de cintilação (fica só o campo estático). */
  twinkle?: boolean
  className?: string
}

export function Starfield({ density = 'densa', twinkle = true, className }: StarfieldProps) {
  const cintilantes = twinkle ? ESTRELAS.slice(0, CINTILANTES_POR_DENSIDADE[density]) : []

  return (
    <div
      aria-hidden="true"
      className={cn('pointer-events-none absolute inset-0 overflow-hidden', className)}
    >
      <div className={CLASSE_CAMPO[density]} />
      {cintilantes.map((estrela) => (
        <span
          key={`${estrela.top}-${estrela.left}`}
          className={cn('ci-estrela', estrela.glow && 'ci-estrela-glow')}
          style={
            {
              top: estrela.top,
              left: estrela.left,
              width: estrela.lado,
              height: estrela.lado,
              ...(estrela.azul ? { background: 'rgb(var(--estrela-azul-rgb))' } : null),
              '--twinkle-dur': estrela.duracao,
              '--twinkle-delay': estrela.atraso ?? '0s',
            } as CSSProperties
          }
        />
      ))}
    </div>
  )
}
