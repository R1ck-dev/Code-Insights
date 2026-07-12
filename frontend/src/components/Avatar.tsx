import { cn, initials } from '@/lib/utils'

interface AvatarProps {
  name: string | null | undefined
  /** Lado do quadrado. Usos canônicos: 27 (topbar) · 32 (rodapé da sidebar) · 44 (card de autor) · 60–72 (perfil). */
  size?: number
  className?: string
}

/**
 * Monograma neutro (spec 01 §7.10): `elevated` + hairline `line-strong`, iniciais
 * em IBM Plex Mono 600 na cor `ink`. Raio 3px, como tudo.
 *
 * Sem gradiente, sem cor de marca — no Órbita a identidade da pessoa é osso/tinta.
 * O peso é `font-semibold` (600) via classe, então um consumidor pode passar
 * `className="font-bold"` (ex.: card de autor da Explorar, mono 15/700).
 */
export function Avatar({ name, size = 34, className }: AvatarProps) {
  // Calibrado contra o markup: 27→11 · 32→12 · 44→15.
  const fontSize = Math.min(15, Math.max(10, Math.round(size * 0.39)))

  return (
    <span
      className={cn(
        'flex shrink-0 items-center justify-center rounded-ci border border-line-strong bg-elevated font-mono font-semibold text-ink',
        className,
      )}
      style={{ width: size, height: size, fontSize }}
      aria-hidden="true"
    >
      {initials(name)}
    </span>
  )
}
