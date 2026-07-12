import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Logo } from '@/components/Logo'
import { Nebula } from '@/components/Nebula'
import type { NebulaVariant } from '@/components/Nebula'
import { Starfield } from '@/components/Starfield'
import type { StarfieldDensity } from '@/components/Starfield'
import { ThemeToggle } from '@/components/ThemeToggle'
import { cn } from '@/lib/utils'

/*
 * Moldura das telas de acesso · ÓRBITA (spec 03 §0.4 e §A).
 *
 * É AQUI que o céu aparece: `Nebula` + `Starfield` sobre `bg`. Cenário — `aria-hidden`,
 * `pointer-events:none`, desligado sob `prefers-reduced-motion` (os componentes já cuidam disso).
 * O glow radial índigo e a grade de pontos da identidade antiga foram removidos.
 *
 * Larguras canônicas do cartão (§00-INDICE §5, divergência 13):
 *   424 → A · E · H · I   ·   440 → F   ·   380 → G
 */

/** Largura do cartão central. */
export type AuthCardWidth = 424 | 440 | 380

interface AuthCardProps {
  width?: AuthCardWidth | number
  children: ReactNode
  className?: string
}

/**
 * Cartão de acesso: `panel` + hairline `line` + raio 3px + sombra profunda.
 * O `AuthLayout` já o renderiza — só use direto quando a tela precisar de mais de um
 * cartão (ex.: G · Conta ativada) e passar `card={false}` ao layout.
 */
export function AuthCard({ width = 424, children, className }: AuthCardProps) {
  return (
    <div
      className={cn(
        'flex w-full flex-col gap-[18px] rounded-ci border border-line bg-panel p-[30px] shadow-modal-deep',
        className,
      )}
      style={{ maxWidth: width }}
    >
      {children}
    </div>
  )
}

interface AuthLayoutProps {
  children: ReactNode
  /** Largura do cartão central (ignorada quando `card={false}`). */
  width?: AuthCardWidth | number
  /**
   * `false` → o layout entrega só o céu + o header e centraliza os `children` crus
   * (a tela monta os próprios cartões — G tem dois).
   */
  card?: boolean
  /** Nebulosa do céu: `login` (dupla) na tela A · `auth` (índigo no topo) nas demais. */
  nebula?: Extract<NebulaVariant, 'auth' | 'login'>
  /** Densidade do campo de estrelas: `media` (10) no Login · `esparsa` (5) nas demais. */
  estrelas?: Extract<StarfieldDensity, 'media' | 'esparsa'>
  /** Ação no canto superior direito. Default: alternador de tema. */
  topRight?: ReactNode
}

export function AuthLayout({
  children,
  width = 424,
  card = true,
  nebula = 'auth',
  estrelas = 'esparsa',
  topRight,
}: AuthLayoutProps) {
  return (
    <div className="relative flex min-h-screen flex-col overflow-hidden bg-bg">
      <Nebula variant={nebula} />
      <Starfield density={estrelas} />

      <header className="relative z-10 flex h-[58px] shrink-0 items-center justify-between px-6 sm:px-8">
        <Link to="/" aria-label="Início" className="ci-foco-botao rounded-ci">
          <Logo size={26} />
        </Link>
        {topRight ?? <ThemeToggle size={36} />}
      </header>

      {/* pt-[18px]: compensação óptica do protótipo (o cartão desce ~18px do centro geométrico) */}
      <main className="relative z-10 flex flex-1 items-center justify-center px-4 pb-16 pt-[18px]">
        {card ? <AuthCard width={width}>{children}</AuthCard> : children}
      </main>
    </div>
  )
}
