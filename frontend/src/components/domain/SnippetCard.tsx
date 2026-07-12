/*
 * Card de snippet — tela O (Snippets) e qualquer grade de trechos.
 *
 * Composição: CodeBlock compacto (sem números de linha, com "Copiar" — copiar É o ponto
 * de um snippet) + rodapé de meta: chip da categoria · desafio vinculado (opcional) ·
 * data mono · ações (opcional).
 *
 * O modelo não tem "título": a DESCRIÇÃO é o nome do snippet e vira o rótulo do CodeBlock
 * (fallback: o nome da categoria). Por isso ela não se repete no rodapé.
 *
 * NÃO é um link: o CodeBlock já traz um botão ("Copiar") e as ações são interativas —
 * card clicável aninharia controles. Quem quiser abrir o detalhe usa `actions`.
 */
import { Clock, Target } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { CodeBlock, type CodeLang } from '@/components/CodeBlock'
import { Chip } from '@/components/ui/badge'
import { CATEGORIA_META } from '@/domain/enums'
import { cn, formatDate } from '@/lib/utils'
import type { SnippetDTO } from '@/types/api'

export interface SnippetCardProps {
  snippet: SnippetDTO
  /** Linguagem do realce. O snippet não guarda linguagem no backend — default `java`. */
  lang?: CodeLang
  /** Título do desafio vinculado (`snippet.desafioId`). Sem ele, o vínculo não é exibido. */
  desafioTitulo?: string | null
  /** Rota do desafio vinculado. Sem ela, o vínculo vira texto (não-clicável). */
  desafioHref?: string
  /** Ações do dono (editar / remover) — botões `iconOnly size="sm"`. */
  actions?: ReactNode
  /** Altura máxima do trecho de código. Default 128px (a célula da tela O). */
  maxHeight?: number
  className?: string
}

export function SnippetCard({
  snippet,
  lang = 'java',
  desafioTitulo,
  desafioHref,
  actions,
  maxHeight = 128,
  className,
}: SnippetCardProps) {
  const categoria = CATEGORIA_META[snippet.categoria]
  const titulo = snippet.descricao?.trim() || categoria?.label || 'Snippet'

  return (
    <article
      className={cn(
        'flex h-full flex-col gap-2.5 rounded-ci border border-line bg-panel p-3.5',
        'transition-colors hover:border-line-strong',
        className,
      )}
    >
      <CodeBlock
        code={snippet.codigo}
        lang={lang}
        label={titulo}
        lines={false}
        maxHeight={maxHeight}
      />

      <div className="flex flex-wrap items-center gap-2">
        {categoria && (
          <Chip icon={categoria.icon} className="px-2 py-[3px] text-[11px]">
            {categoria.label}
          </Chip>
        )}
        {desafioTitulo && <DesafioVinculado titulo={desafioTitulo} href={desafioHref} />}
      </div>

      <div className="mt-auto flex items-center justify-between gap-2 border-t border-line-soft pt-2.5">
        <span className="flex items-center gap-1.5 text-soft">
          <Clock size={13} strokeWidth={2} aria-hidden className="shrink-0" />
          <time dateTime={snippet.criadoEm} className="tabular font-mono text-[11px]">
            criado em {formatDate(snippet.criadoEm)}
          </time>
        </span>
        {actions && <span className="flex shrink-0 items-center gap-1.5">{actions}</span>}
      </div>
    </article>
  )
}

/** Vínculo snippet ↔ desafio (opcional, `SET NULL` no backend). */
function DesafioVinculado({ titulo, href }: { titulo: string; href?: string }) {
  const conteudo = (
    <>
      <Target size={12} strokeWidth={2} aria-hidden className="shrink-0" />
      <span className="truncate">{titulo}</span>
    </>
  )
  const classes = 'flex min-w-0 items-center gap-1.5 font-mono text-[11px]'

  return href ? (
    <Link to={href} className={cn(classes, 'text-steel hover:text-steel-hover')}>
      {conteudo}
    </Link>
  ) : (
    <span className={cn(classes, 'text-soft')}>{conteudo}</span>
  )
}
