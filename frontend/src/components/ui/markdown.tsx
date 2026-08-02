import { Fragment, type ReactNode } from 'react'
import { cn } from '@/lib/utils'

/*
 * Renderizador de markdown mínimo — ÓRBITA.
 *
 * Existe em vez de uma dependência porque o único texto que passa por aqui é o TCLE, cujo markdown
 * é um subconjunto conhecido: títulos, citação, lista, negrito, itálico, código e régua. Somar
 * ~100 kB ao bundle por uma tela não se paga, e ter o parser aqui deixa a tipografia dentro do
 * sistema em vez de brigar com o CSS de um pacote.
 *
 * A regra que sustenta isso: NADA É DESCARTADO. Uma linha que não casa com nenhum bloco conhecido é
 * renderizada como parágrafo, literalmente. Num documento que a pessoa está aceitando, silenciar
 * uma linha que o parser não entendeu seria o pior defeito possível — pior que exibi-la torta.
 *
 * Sem `dangerouslySetInnerHTML`: o texto vira nós React, então não há caminho para HTML injetado
 * mesmo que o arquivo mude de mãos.
 */

export function Markdown({ texto, className }: { texto: string; className?: string }) {
  return (
    <div className={cn('flex flex-col gap-[13px]', className)}>
      {blocos(texto).map((bloco, i) => (
        <Bloco key={i} bloco={bloco} />
      ))}
    </div>
  )
}

/* ------------------------------------------------------------------ blocos --- */

type Bloco =
  | { tipo: 'titulo'; nivel: 1 | 2 | 3; texto: string }
  | { tipo: 'citacao'; linhas: string[] }
  | { tipo: 'lista'; itens: string[] }
  | { tipo: 'regua' }
  | { tipo: 'paragrafo'; texto: string }

function blocos(fonte: string): Bloco[] {
  const linhas = fonte.replace(/\r\n/g, '\n').split('\n')
  const saida: Bloco[] = []
  let i = 0

  while (i < linhas.length) {
    const linha = linhas[i]

    if (linha.trim() === '') {
      i++
      continue
    }

    if (/^-{3,}$/.test(linha.trim())) {
      saida.push({ tipo: 'regua' })
      i++
      continue
    }

    const titulo = /^(#{1,3})\s+(.*)$/.exec(linha)
    if (titulo) {
      saida.push({ tipo: 'titulo', nivel: titulo[1].length as 1 | 2 | 3, texto: titulo[2] })
      i++
      continue
    }

    if (linha.startsWith('> ') || linha.trim() === '>') {
      const bloco: string[] = []
      while (i < linhas.length && (linhas[i].startsWith('> ') || linhas[i].trim() === '>')) {
        bloco.push(linhas[i].replace(/^>\s?/, ''))
        i++
      }
      saida.push({ tipo: 'citacao', linhas: bloco })
      continue
    }

    if (/^\s*[-*]\s+/.test(linha)) {
      const itens: string[] = []
      // Linha seguinte indentada continua o item anterior — é como o markdown quebra item longo.
      while (i < linhas.length && (/^\s*[-*]\s+/.test(linhas[i]) || /^\s{2,}\S/.test(linhas[i]))) {
        if (/^\s*[-*]\s+/.test(linhas[i])) itens.push(linhas[i].replace(/^\s*[-*]\s+/, ''))
        else itens[itens.length - 1] += ' ' + linhas[i].trim()
        i++
      }
      saida.push({ tipo: 'lista', itens })
      continue
    }

    // Parágrafo: junta até a linha em branco. Qualquer coisa não reconhecida cai aqui, literal.
    const paragrafo: string[] = []
    while (i < linhas.length && linhas[i].trim() !== '' && !ehInicioDeOutroBloco(linhas[i])) {
      paragrafo.push(linhas[i].trim())
      i++
    }
    saida.push({ tipo: 'paragrafo', texto: paragrafo.join(' ') })
  }

  return saida
}

function ehInicioDeOutroBloco(linha: string): boolean {
  return (
    /^#{1,3}\s/.test(linha) ||
    linha.startsWith('>') ||
    /^\s*[-*]\s+/.test(linha) ||
    /^-{3,}$/.test(linha.trim())
  )
}

function Bloco({ bloco }: { bloco: Bloco }) {
  switch (bloco.tipo) {
    case 'regua':
      return <hr className="border-t border-line" />

    case 'titulo': {
      const Tag = (['h2', 'h3', 'h4'] as const)[bloco.nivel - 1]
      const escala = ['text-[19px]', 'text-[15.5px]', 'text-[13.5px]'][bloco.nivel - 1]
      return (
        <Tag className={cn('font-semibold text-ink', escala, bloco.nivel > 1 && 'mt-[9px]')}>
          {inline(bloco.texto)}
        </Tag>
      )
    }

    /** Usado no termo para o aviso de rascunho — precisa saltar aos olhos, não passar batido. */
    case 'citacao':
      return (
        <blockquote className="flex flex-col gap-[7px] rounded-ci border border-atencao-line bg-atencao-bg px-[15px] py-[13px]">
          {bloco.linhas
            .join('\n')
            .split(/\n\s*\n/)
            .map((paragrafo, i) => (
              <p key={i} className="text-[13px] leading-[1.6] text-atencao-ink">
                {inline(paragrafo.replace(/\n/g, ' '))}
              </p>
            ))}
        </blockquote>
      )

    case 'lista':
      return (
        <ul className="flex flex-col gap-[7px] pl-[19px]">
          {bloco.itens.map((item, i) => (
            <li key={i} className="list-disc text-[13.5px] leading-[1.65] text-body marker:text-soft">
              {inline(item)}
            </li>
          ))}
        </ul>
      )

    case 'paragrafo':
      return <p className="text-[13.5px] leading-[1.7] text-body">{inline(bloco.texto)}</p>
  }
}

/* ------------------------------------------------------------------ inline --- */

const MARCACOES = /(\*\*[^*]+\*\*|_[^_]+_|`[^`]+`)/g

function inline(texto: string): ReactNode {
  const partes: ReactNode[] = []
  let cursor = 0
  let achado: RegExpExecArray | null

  MARCACOES.lastIndex = 0
  while ((achado = MARCACOES.exec(texto)) !== null) {
    if (achado.index > cursor) partes.push(texto.slice(cursor, achado.index))
    partes.push(<Marcado key={achado.index} bruto={achado[0]} />)
    cursor = achado.index + achado[0].length
  }
  if (cursor < texto.length) partes.push(texto.slice(cursor))

  return partes.map((parte, i) => <Fragment key={i}>{parte}</Fragment>)
}

function Marcado({ bruto }: { bruto: string }) {
  if (bruto.startsWith('**')) return <strong className="font-semibold text-ink">{bruto.slice(2, -2)}</strong>
  if (bruto.startsWith('`')) {
    return (
      <code className="rounded-ci-sm bg-recess px-[5px] py-[1px] font-mono text-[12px] text-ink">
        {bruto.slice(1, -1)}
      </code>
    )
  }
  return <em className="text-mid italic">{bruto.slice(1, -1)}</em>
}
