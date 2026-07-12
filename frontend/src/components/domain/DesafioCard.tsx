/*
 * Card de desafio — telas L (meus desafios), J (portfólio público) e Explorar.
 *
 * O card INTEIRO é um <Link> de verdade (não `role="button"` + onKeyDown): teclado,
 * leitor de tela, "abrir em nova aba" e o menu de contexto vêm de graça.
 * Nada de interativo aninhado aqui dentro — chips e datas são conteúdo, não controles.
 *
 * Forma: panel + hairline 1px, raio 3px, hover eleva a borda para `line-strong`.
 * Sem sombra. Sem cor fora do colormap (o card não carrega métrica).
 */
import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { StatusChip } from '@/components/domain/badges'
import { Chip } from '@/components/ui/badge'
import { cn, formatDate, pluralPt } from '@/lib/utils'
import type { Visibilidade } from '@/types/api'

export interface DesafioCardProps {
  /** Destino do card inteiro. A tela escolhe a rota: `/app/desafios/:id` (L) ou `/u/:autorId/desafios/:id` (J). */
  to: string
  titulo: string
  /** Plataforma de origem (`LeetCode`, `Codeforces`…). */
  plataforma?: string | null
  /** Identificador externo na plataforma (`#1`, `#204`, `1200C`). */
  identificador?: string | null
  /** Chip de visibilidade. Omita em contexto público (tela J), onde tudo já é público. */
  visibilidade?: Visibilidade | null
  /** `null`/`undefined` → a contagem some (o DTO de lista não a traz). `0` → `Sem resoluções`. */
  qtdResolucoes?: number | null
  /** ISO. Renderizado em mono `dd/mm/aaaa`. */
  criadoEm?: string | null
  /** Chevron de "entrar" no rodapé — o portfólio público (tela J) usa. */
  chevron?: boolean
  className?: string
}

export function DesafioCard({
  to,
  titulo,
  plataforma,
  identificador,
  visibilidade,
  qtdResolucoes,
  criadoEm,
  chevron = false,
  className,
}: DesafioCardProps) {
  const temMeta = Boolean(plataforma || identificador)

  return (
    <Link
      to={to}
      className={cn(
        'group flex h-full flex-col gap-3 rounded-ci border border-line bg-panel p-[17px]',
        'transition-colors hover:border-line-strong',
        className,
      )}
    >
      <div className="flex items-start justify-between gap-2.5">
        <h3 className="line-clamp-2 text-[15px] leading-snug font-semibold text-ink">{titulo}</h3>
        {visibilidade && (
          <StatusChip status={visibilidade === 'PUBLICO' ? 'publico' : 'privado'} />
        )}
      </div>

      {temMeta && (
        <div className="flex flex-wrap items-center gap-2">
          {plataforma && (
            <Chip className="px-2 py-[3px] text-[11px] text-mid">{plataforma}</Chip>
          )}
          {identificador && (
            <span className="tabular font-mono text-[11px] text-soft">{identificador}</span>
          )}
        </div>
      )}

      <div className="mt-auto flex items-center justify-between gap-2 border-t border-line-soft pt-[11px]">
        <Contagem qtd={qtdResolucoes} />
        <span className="flex shrink-0 items-center gap-2">
          {criadoEm && (
            <time dateTime={criadoEm} className="tabular font-mono text-[11px] text-soft">
              {formatDate(criadoEm)}
            </time>
          )}
          {chevron && (
            <ChevronRight
              size={15}
              strokeWidth={2}
              aria-hidden
              className="shrink-0 text-steel transition-colors group-hover:text-steel-hover"
            />
          )}
        </span>
      </div>
    </Link>
  )
}

/** `null` → nada (a lista não trouxe a contagem) · `0` → `Sem resoluções` em `soft`. */
function Contagem({ qtd }: { qtd?: number | null }) {
  if (qtd == null) return <span />
  if (qtd === 0) return <span className="text-[12px] text-soft">Sem resoluções</span>
  return (
    <span className="tabular text-[12px] text-mid">
      {pluralPt(qtd, 'resolução', 'resoluções')}
    </span>
  )
}
