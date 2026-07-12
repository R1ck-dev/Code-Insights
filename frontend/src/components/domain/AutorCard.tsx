/*
 * Card de autor — tela Explorar (`/explorar` público e `/app/explorar` autenticado).
 * Receita: 00-INDICE §4.2 (cruzamento do card de desafio da tela L com o header de autor da J).
 *
 * Card inteiro é um <Link> para `/u/:usuarioId`. Nada interativo aninhado.
 * A autonomia média usa o AutonomyMeter — NEUTRO por regra, nunca colormap.
 */
import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { Avatar } from '@/components/Avatar'
import { cn, pluralPt } from '@/lib/utils'

export interface AutorCardProps {
  usuarioId: string
  username: string
  /** `null`/`undefined` → a linha de stats some (o DTO de listagem pode não trazer). */
  totalDesafios?: number | null
  totalResolucoes?: number | null
  /** Média do Índice de Autonomia IA (1–5, decimal). `null` → o medidor dá lugar a "ver portfólio". */
  autonomiaMedia?: number | null
  className?: string
}

export function AutorCard({
  usuarioId,
  username,
  totalDesafios,
  totalResolucoes,
  autonomiaMedia,
  className,
}: AutorCardProps) {
  const stats = [
    totalDesafios != null ? pluralPt(totalDesafios, 'desafio', 'desafios') : null,
    totalResolucoes != null ? pluralPt(totalResolucoes, 'resolução', 'resoluções') : null,
  ].filter(Boolean)

  return (
    <Link
      to={`/u/${usuarioId}`}
      className={cn(
        'group flex h-full flex-col gap-[13px] rounded-ci border border-line bg-panel p-[17px]',
        'transition-colors hover:border-line-strong',
        className,
      )}
    >
      <div className="flex min-w-0 items-center gap-3">
        <Avatar name={username} size={44} className="font-bold" />
        <div className="flex min-w-0 flex-col gap-1">
          <span className="truncate text-[15px] font-semibold text-ink">@{username}</span>
          {stats.length > 0 && (
            <span className="tabular truncate font-mono text-[11px] text-soft">
              {stats.join(' · ')}
            </span>
          )}
        </div>
      </div>

      <div className="mt-auto flex items-center justify-between gap-2 border-t border-line-soft pt-[11px]">
        {autonomiaMedia != null ? (
          <span className="flex items-center gap-2.5">
            <AutonomyMeter value={autonomiaMedia} size="sm" showLabel={false} />
            <span aria-hidden className="tabular font-mono text-[11px] font-semibold text-mid">
              {formataMedia(autonomiaMedia)}/5
            </span>
          </span>
        ) : (
          <span className="font-mono text-[11px] text-soft">ver portfólio</span>
        )}
        <ChevronRight
          size={15}
          strokeWidth={2}
          aria-hidden
          className="shrink-0 text-steel transition-colors group-hover:text-steel-hover"
        />
      </div>
    </Link>
  )
}

/** Decimal em pt-BR: 3.8 → `3,8`. */
function formataMedia(v: number): string {
  return v.toFixed(1).replace('.', ',')
}
