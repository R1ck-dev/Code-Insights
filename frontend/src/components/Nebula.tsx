import type { CSSProperties } from 'react'
import { cn } from '@/lib/utils'

/**
 * Nebulosa (spec 01 §6.2). Radial-gradients profundos de céu — azul-índigo (NE),
 * violeta (SO) e azul-aço (centro), alphas .08–.22.
 *
 * ⚠ REGRA 1 (colormap é a única cor) continua valendo: a nebulosa é CENÁRIO — fundo
 * de TELA/painel, a única licença poética do sistema. NUNCA aplicar em cartão, botão,
 * chip ou qualquer componente.
 *
 * É uma camada ADITIVA (`absolute inset-0`, sem cor de base opaca): o pai mantém o
 * próprio fundo e precisa ser `relative`. Os canais vêm dos tokens `--nebula-*-rgb` e
 * a intensidade de `--nebula-forca` — no claro a nebulosa vira tinta discreta sozinha.
 *
 * (As classes `.ci-nebula-*` do index.css fazem o mesmo pintando o próprio container.
 * Use uma OU outra — nunca as duas empilhadas.)
 */

export type NebulaVariant =
  | 'auth' // A · E · F · G · H · I — telas de acesso
  | 'login' // A · Login (dupla: índigo no topo + violeta na base)
  | 'landing' // B · hero
  | 'carta' // painel dos gráficos
  | 'painel' // painel sobre `panel` (dashboard)
  | 'portfolio' // J · header do autor
  | 'plate' // plate cheio (as três nebulosas)

const INDIGO = '--nebula-indigo-rgb'
const VIOLETA = '--nebula-violeta-rgb'
const ACO = '--nebula-aco-rgb'

/** [geometria, canal, alpha, stop] — valores literais da spec 01 §6.2. */
type Camada = [string, string, number, string]

const RECEITAS: Record<NebulaVariant, { camadas: Camada[]; forca?: number }> = {
  auth: { camadas: [['70% 55% at 50% -6%', INDIGO, 0.16, '58%']] },
  login: {
    camadas: [
      ['80% 60% at 50% 6%', INDIGO, 0.2, '55%'],
      ['70% 60% at 12% 108%', VIOLETA, 0.14, '55%'],
    ],
  },
  landing: { camadas: [['70% 90% at 88% 0%', INDIGO, 0.16, '58%']] },
  // No claro o markup usa o alpha cheio: força 1 nos dois modos.
  carta: { camadas: [['95% 75% at 84% -12%', INDIGO, 0.16, '55%']], forca: 1 },
  painel: { camadas: [['120% 90% at 88% -10%', INDIGO, 0.1, '55%']] },
  portfolio: { camadas: [['70% 130% at 8% -30%', INDIGO, 0.14, '60%']] },
  plate: {
    camadas: [
      ['85% 65% at 84% -14%', INDIGO, 0.22, '55%'],
      ['80% 78% at 6% 110%', VIOLETA, 0.16, '55%'],
      ['70% 55% at 40% 42%', ACO, 0.08, '60%'],
    ],
  },
}

interface NebulaProps {
  variant?: NebulaVariant
  className?: string
}

export function Nebula({ variant = 'auth', className }: NebulaProps) {
  const { camadas, forca } = RECEITAS[variant]

  const background = camadas
    .map(
      ([geometria, canal, alpha, stop]) =>
        `radial-gradient(${geometria}, rgba(var(${canal}), calc(${alpha} * var(--nebula-forca))), transparent ${stop})`,
    )
    .join(', ')

  return (
    <div
      aria-hidden="true"
      className={cn('pointer-events-none absolute inset-0', className)}
      style={
        {
          background,
          ...(forca === undefined ? null : { '--nebula-forca': forca }),
        } as CSSProperties
      }
    />
  )
}
