/*
 * RÓTULO ESTÁVEL — texto que não muda de largura quando fica em negrito.
 *
 * PROBLEMA (o usuário viu, no seletor de granularidade da Linha): num segmented control, o item
 * ATIVO é marcado com `font-semibold`. Negrito é mais largo que regular — então o item ativo
 * engorda e EMPURRA os vizinhos: a cada clique a fileira inteira se rearranja, e os botões parecem
 * "trocar de lugar" debaixo do cursor. É o pior tipo de layout shift: o alvo se move no instante
 * exato em que o usuário mira nele.
 *
 * SOLUÇÃO: reservar SEMPRE a largura do negrito. Um gêmeo invisível (`aria-hidden`, sempre em
 * `font-semibold`) ocupa a célula da grade e define a largura; o rótulo real é pintado por cima,
 * na mesma célula. A caixa é a mesma nos dois estados — só a tinta e o peso mudam.
 *
 * Alternativas descartadas: largura fixa em px (quebra com a fonte do sistema, com zoom e com
 * tradução) e tirar o negrito (aí o estado ativo perde metade do seu sinal, ficando só na cor —
 * e cor sozinha não é sinal acessível).
 */
import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

export function RotuloEstavel({
  children,
  className,
}: {
  /** Texto do rótulo. É renderizado DUAS vezes (o gêmeo é invisível) — mantenha-o simples. */
  children: ReactNode
  className?: string
}) {
  return (
    <span className={cn('grid', className)}>
      <span aria-hidden className="invisible col-start-1 row-start-1 font-semibold">
        {children}
      </span>
      <span className="col-start-1 row-start-1">{children}</span>
    </span>
  )
}
