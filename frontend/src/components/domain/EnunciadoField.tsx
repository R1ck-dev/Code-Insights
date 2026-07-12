/*
 * CAMPO DE ENUNCIADO — o `Textarea` que entende uma colagem vinda de juiz online.
 *
 * O fluxo real do aluno é copiar-e-colar do Neps / Codeforces / Beecrowd. Nessas páginas a
 * matemática é renderizada pelo KaTeX/MathJax, que emite CADA FÓRMULA DUAS VEZES no HTML (um ramo
 * MathML invisível, para leitores de tela, e o desenho visual). A área de transferência leva os
 * dois — e o enunciado chega assim: "ler dois valores inteiros A A e B B … expressão (A+B) (A+B)."
 *
 * Este campo intercepta o `paste` e trata o `text/html` do clipboard, onde a duplicata ainda tem
 * nome e classe e pode ser podada com precisão (ver `@/lib/enunciado-colado`). O aluno cola e o
 * texto entra limpo — sem botão para clicar, sem passo para aprender.
 *
 * O caminho degradado é honesto: quando só existe `text/plain` (o usuário passou por um bloco de
 * notas antes, ou o site não expõe HTML), a duplicata já virou texto comum e não há como separá-la
 * com segurança de uma repetição legítima. Aí o campo NÃO adivinha: ele avisa e oferece a limpeza
 * como uma ação explícita. Corrigir sozinho, no escuro, seria pior — pode comer o "5 5" de um
 * exemplo de entrada e ninguém percebe.
 */
import { useState } from 'react'
import { Eraser } from 'lucide-react'
import { Textarea, type TextareaProps } from '@/components/ui/input'
import {
  normalizarTexto,
  removerDuplicacoesAdjacentes,
  temDuplicacaoDeFormula,
  textoDeHtmlColado,
} from '@/lib/enunciado-colado'

export interface EnunciadoFieldProps
  extends Omit<TextareaProps, 'value' | 'onChange' | 'hint'> {
  value: string
  /** Recebe o texto FINAL (já limpo, quando a colagem trouxe HTML). */
  onChange: (valor: string) => void
}

export function EnunciadoField({ value, onChange, ...props }: EnunciadoFieldProps) {
  /** Só aparece quando a colagem veio sem HTML E cheira a fórmula duplicada. */
  const [suspeita, setSuspeita] = useState(false)

  function aoColar(evento: React.ClipboardEvent<HTMLTextAreaElement>) {
    const html = evento.clipboardData.getData('text/html')
    const plano = evento.clipboardData.getData('text/plain')
    if (!html && !plano) return

    const limpo = (html ? textoDeHtmlColado(html) : null) ?? normalizarTexto(plano)
    if (!limpo) return

    // Assumimos a inserção (o default do navegador colaria o texto sujo).
    evento.preventDefault()

    const area = evento.currentTarget
    const inicio = area.selectionStart ?? value.length
    const fim = area.selectionEnd ?? value.length
    onChange(value.slice(0, inicio) + limpo + value.slice(fim))

    // Sem o HTML, a poda não foi possível: o que dá para fazer é apontar o problema.
    setSuspeita(!html && temDuplicacaoDeFormula(limpo))
  }

  return (
    <Textarea
      {...props}
      value={value}
      onChange={(e) => {
        onChange(e.target.value)
        if (suspeita) setSuspeita(false) // o usuário assumiu o texto: o aviso não insiste
      }}
      onPaste={aoColar}
      hint={
        suspeita ? (
          <span className="flex flex-wrap items-center gap-1.5">
            O texto colado parece ter fórmulas repetidas (ex.:{' '}
            <span className="font-mono">A A</span>) — comum ao colar de juízes online.
            <button
              type="button"
              onClick={() => {
                onChange(removerDuplicacoesAdjacentes(value))
                setSuspeita(false)
              }}
              className="ci-foco-botao inline-flex cursor-pointer items-center gap-1 rounded-ci text-steel transition-colors hover:text-steel-hover"
            >
              <Eraser size={12} strokeWidth={2} aria-hidden />
              limpar repetições
            </button>
          </span>
        ) : null
      }
    />
  )
}
