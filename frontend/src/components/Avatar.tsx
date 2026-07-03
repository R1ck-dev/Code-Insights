import { initials } from '@/lib/utils'
import { cn } from '@/lib/utils'

interface AvatarProps {
  name: string | null | undefined
  size?: number
  className?: string
}

/** Monograma em gradiente índigo profundo com as iniciais. */
export function Avatar({ name, size = 34, className }: AvatarProps) {
  return (
    <span
      className={cn('flex shrink-0 items-center justify-center font-mono font-bold', className)}
      style={{
        width: size,
        height: size,
        borderRadius: Math.round(size * 0.26),
        fontSize: Math.round(size * 0.38),
        color: '#C7C3FF',
        background: 'linear-gradient(150deg,#2A2472,#4A3FB5)',
      }}
      aria-hidden
    >
      {initials(name)}
    </span>
  )
}
