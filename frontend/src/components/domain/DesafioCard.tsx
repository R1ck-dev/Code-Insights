/*
 * Card de desafio — telas L (meus desafios), J (portfólio público) e Explorar.
 *
 * O card continua sendo UM alvo só: clicar em qualquer lugar abre o desafio, com teclado, leitor
 * de tela, "abrir em nova aba" e menu de contexto — porque continua sendo um `<Link>` de verdade.
 *
 * ⚠ MUDOU A MONTAGEM (para o chip de visibilidade poder ALTERNAR — pedido do usuário: trocar
 * público/privado sem entrar tela por tela). Antes o `<Link>` embrulhava o card inteiro, e ali um
 * botão não cabe: `<button>` dentro de `<a>` é HTML inválido, o clique navegaria em vez de
 * alternar e o leitor de tela anunciaria um link contendo um botão. Agora o link é uma CAMADA
 * ESTICADA (`absolute inset-0`) sobre um `<article>`, e o chip fica por cima dela (`z-10`). O card
 * inteiro segue clicável; o chip é a única ilha que não navega.
 *
 * Sem `onAlternarVisibilidade` (portfólio público, Explorar), o chip volta a ser um rótulo mudo —
 * lá não há o que alternar: o desafio é de outra pessoa.
 *
 * Forma: panel + hairline 1px, raio 3px, hover eleva a borda para `line-strong`.
 * Sem sombra. Sem cor fora do colormap (o card não carrega métrica).
 */
import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { StatusChip, VisibilidadeToggle } from '@/components/domain/badges'
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
  /**
   * Torna o chip de visibilidade um BOTÃO que alterna (só faz sentido nos MEUS desafios). Recebe
   * o valor novo. Sem ele, o chip é um rótulo, como sempre foi.
   */
  onAlternarVisibilidade?: (publico: boolean) => void
  /** A alternância está em voo: spinner no chip, clique bloqueado. */
  alterandoVisibilidade?: boolean
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
  onAlternarVisibilidade,
  alterandoVisibilidade,
  className,
}: DesafioCardProps) {
  const temMeta = Boolean(plataforma || identificador)

  return (
    <article
      className={cn(
        'group relative flex h-full flex-col gap-3 rounded-ci border border-line bg-panel p-[17px]',
        'transition-colors hover:border-line-strong focus-within:border-line-strong',
        className,
      )}
    >
      {/*
       * O LINK ESTICADO: cobre o card inteiro (por ser `absolute`, ele pinta ACIMA do texto, que é
       * estático) e leva o anel de foco do sistema. É ele que faz "clicar em qualquer lugar abre o
       * desafio" continuar valendo depois que o card deixou de ser um `<a>` por fora.
       */}
      <Link
        to={to}
        aria-label={`Abrir ${titulo}`}
        className="ci-foco-botao absolute inset-0 z-0 rounded-ci"
      />

      <div className="flex items-start justify-between gap-2.5">
        <h3 className="line-clamp-2 text-[15px] leading-snug font-semibold text-ink">{titulo}</h3>
        {visibilidade &&
          (onAlternarVisibilidade ? (
            <VisibilidadeToggle
              publico={visibilidade === 'PUBLICO'}
              onAlternar={onAlternarVisibilidade}
              pendente={alterandoVisibilidade}
            />
          ) : (
            <StatusChip status={visibilidade === 'PUBLICO' ? 'publico' : 'privado'} />
          ))}
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
    </article>
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
