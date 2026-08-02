import { cn } from '@/lib/utils'

/*
 * Tabela · ÓRBITA — vocabulário NOVO, introduzido para a área de pesquisa.
 *
 * O sistema resolvia dado tabular com grade de cards ou lista de linhas, e isso continua certo
 * para as telas do aluno: lá cada item é uma coisa a abrir. Aqui é o oposto — a tela de coorte
 * existe para COMPARAR ~9 campos entre dezenas de linhas, e alinhamento vertical de coluna é o
 * único jeito de fazer isso. `<table>` com `<th scope="col">` também é o marcado correto para
 * leitor de tela, o que uma grade de <div> nunca é.
 *
 * Sem cor própria: hairline `line`, `panel` de fundo, mono para DADO, `.tabular` em número.
 * A única cor que pode entrar numa célula é a do colormap de complexidade, vinda de quem usa.
 */

/**
 * Moldura + região de rolagem própria: a página nunca rola no eixo X por causa da tabela.
 *
 * O `max-h` não é decoração — é o que faz o `sticky` do `<thead>` existir. Um box com
 * `overflow-x: auto` já computa `overflow-y: auto`, ou seja, este div é o scrollport mais próximo do
 * cabeçalho quer queiramos ou não; sem altura máxima ele cresce com o conteúdo, nunca rola, e o
 * sticky fica com deslocamento zero para sempre. Quem rolaria seria a página, e o cabeçalho subiria
 * junto. Dar altura ao container é o que devolve o comportamento prometido em `TableHead`.
 */
export function Table({
  className,
  children,
  ...props
}: React.TableHTMLAttributes<HTMLTableElement>) {
  return (
    <div className="max-h-[70vh] overflow-auto rounded-ci border border-line bg-panel">
      <table className={cn('w-full border-collapse text-left', className)} {...props}>
        {children}
      </table>
    </div>
  )
}

/** Cabeçalho grudado no topo: rolar 40 linhas sem saber que coluna é qual é rolar às cegas. */
export function TableHead({ className, ...props }: React.HTMLAttributes<HTMLTableSectionElement>) {
  return <thead className={cn('sticky top-0 z-10 bg-panel', className)} {...props} />
}

export function TableBody({ className, ...props }: React.HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <tbody
      className={cn('[&>tr:last-child>td]:border-b-0', className)}
      {...props}
    />
  )
}

export function TableRow({ className, ...props }: React.HTMLAttributes<HTMLTableRowElement>) {
  return <tr className={cn('transition-colors hover:bg-elevated', className)} {...props} />
}

interface CelulaProps extends React.TdHTMLAttributes<HTMLTableCellElement> {
  /** Alinha à direita e liga `.tabular`: dígito de número tem que alinhar com o de cima. */
  numerico?: boolean
}

export function TableHeaderCell({ className, numerico, ...props }: CelulaProps) {
  return (
    <th
      scope="col"
      className={cn(
        'border-b border-line px-[11px] py-[9px] font-mono text-[10.5px] font-semibold tracking-[.06em] whitespace-nowrap text-mid uppercase',
        numerico && 'text-right',
        className,
      )}
      {...props}
    />
  )
}

export function TableCell({ className, numerico, ...props }: CelulaProps) {
  return (
    <td
      className={cn(
        'border-b border-line-soft px-[11px] py-[9px] text-[12.5px] whitespace-nowrap text-body',
        numerico && 'tabular text-right font-mono',
        className,
      )}
      {...props}
    />
  )
}
