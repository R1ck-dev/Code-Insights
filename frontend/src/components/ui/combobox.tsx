/*
 * COMBOBOX — campo de texto LIVRE com sugestões filtradas pela digitação.
 *
 * Não é um `Select`. A diferença importa: no Select o valor tem de estar na lista; aqui a lista é
 * só um atalho. O aluno digita "nep", vê "Neps Academy" e escolhe; mas se o desafio veio da lista
 * da disciplina, ele escreve "Lista 3 - POO" e isso vale. É o requisito do campo Plataforma —
 * sugerir sem proibir (ver `@/domain/plataformas`), e por isso não existe uma opção "Outro": ela
 * seria um passo a mais para dizer o que o campo já aceita.
 *
 * ⚠ Não usei o Radix aqui: ele não traz combobox (só Select, que é de valor fechado), e uma
 * `<datalist>` nativa desenharia o menu com o tema do NAVEGADOR — branco, no meio de uma interface
 * escura, e com comportamento diferente em cada um.
 *
 * ACESSIBILIDADE (padrão ARIA 1.2 "combobox with listbox popup"): `role="combobox"` no input,
 * `aria-expanded`, `aria-controls` e `aria-activedescendant` apontando para a opção em destaque —
 * o leitor de tela anuncia a opção sem que o foco saia do campo (o foco NUNCA vai para a lista;
 * é isso que mantém a digitação viva). Teclado: ↓/↑ navegam, Enter escolhe, Esc fecha e devolve o
 * texto digitado, Tab fecha sem escolher.
 */
import { useEffect, useId, useMemo, useRef, useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { cn } from '@/lib/utils'
import { Campo, controle, estados } from './input'

export interface ComboboxProps {
  id?: string
  label?: string
  hint?: React.ReactNode
  error?: string | null
  required?: boolean
  placeholder?: string
  maxLength?: number
  /** Texto do campo. É o valor final — inclusive quando não está entre as `opcoes`. */
  value: string
  onChange: (valor: string) => void
  /** As sugestões. Filtradas pelo que foi digitado; nenhuma delas é obrigatória. */
  opcoes: readonly string[]
  className?: string
}

export function Combobox({
  id,
  label,
  hint,
  error,
  required,
  placeholder,
  maxLength,
  value,
  onChange,
  opcoes,
  className,
}: ComboboxProps) {
  const auto = useId()
  const campoId = id ?? auto
  const listaId = `${campoId}-lista`

  const [aberto, setAberto] = useState(false)
  /** Índice em destaque na lista. `-1` = nenhum: o que vale é o texto digitado. */
  const [destaque, setDestaque] = useState(-1)
  const raiz = useRef<HTMLDivElement>(null)

  /*
   * Filtro por SUBSTRING, sem acento e sem caixa: "nep" acha "Neps Academy", "força" acharia
   * "Forca". Substring (e não prefixo) porque ninguém lembra se o juiz é "UVa Online Judge" ou
   * "Online Judge da UVa" — mas todo mundo lembra de um pedaço.
   *
   * ⚠ O texto do campo NÃO filtra quando ele é exatamente uma opção (o usuário acabou de escolher):
   * ali a lista mostraria um item só, e reabrir o menu para trocar de ideia viraria um beco.
   */
  const filtradas = useMemo(() => {
    const alvo = normalizar(value)
    if (!alvo || opcoes.some((o) => normalizar(o) === alvo)) return opcoes
    return opcoes.filter((o) => normalizar(o).includes(alvo))
  }, [opcoes, value])

  // Clique fora fecha. (O `blur` do input não serve: clicar numa opção tira o foco do campo.)
  useEffect(() => {
    if (!aberto) return
    const aoClicar = (e: MouseEvent) => {
      if (!raiz.current?.contains(e.target as Node)) setAberto(false)
    }
    document.addEventListener('mousedown', aoClicar)
    return () => document.removeEventListener('mousedown', aoClicar)
  }, [aberto])

  function escolher(opcao: string) {
    onChange(opcao)
    setAberto(false)
    setDestaque(-1)
  }

  function aoTeclar(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault()
      if (!aberto) {
        setAberto(true)
        setDestaque(0)
        return
      }
      const passo = e.key === 'ArrowDown' ? 1 : -1
      const total = filtradas.length
      if (total === 0) return
      setDestaque((atual) => (atual + passo + total) % total)
      return
    }
    if (e.key === 'Enter' && aberto && destaque >= 0 && filtradas[destaque]) {
      // Só intercepta o Enter quando há uma opção EM DESTAQUE: sem isso, o Enter que deveria
      // enviar o formulário seria engolido por um menu aberto que o usuário estava ignorando.
      e.preventDefault()
      escolher(filtradas[destaque])
      return
    }
    if (e.key === 'Escape' && aberto) {
      e.preventDefault() // não deixa o Esc fechar o diálogo inteiro: aqui ele fecha o menu
      setAberto(false)
      setDestaque(-1)
      return
    }
    if (e.key === 'Tab') setAberto(false)
  }

  return (
    <Campo id={campoId} label={label} hint={hint} error={error} required={required}>
      <div ref={raiz} className={cn('relative', className)}>
        <input
          id={campoId}
          role="combobox"
          aria-expanded={aberto}
          aria-controls={listaId}
          aria-autocomplete="list"
          aria-activedescendant={
            aberto && destaque >= 0 ? `${listaId}-${destaque}` : undefined
          }
          aria-invalid={error ? true : undefined}
          autoComplete="off"
          required={required}
          placeholder={placeholder}
          maxLength={maxLength}
          value={value}
          onChange={(e) => {
            onChange(e.target.value)
            setAberto(true)
            setDestaque(-1) // digitar volta a mandar: o destaque anterior não vale mais
          }}
          onFocus={() => setAberto(true)}
          onKeyDown={aoTeclar}
          className={cn(controle, estados, 'h-[40px] pl-[12px] pr-[34px] font-sans text-[13px]')}
        />

        {/* Só um AFETO visual do menu: o campo já abre no foco e na digitação. `tabIndex={-1}`
            para não criar uma segunda parada de tabulação sobre o mesmo controle. */}
        <button
          type="button"
          tabIndex={-1}
          aria-hidden
          onClick={() => setAberto((a) => !a)}
          className="absolute inset-y-0 right-0 flex w-[32px] cursor-pointer items-center justify-center text-soft transition-colors hover:text-ink"
        >
          <ChevronDown
            size={15}
            strokeWidth={2}
            className={cn('transition-transform', aberto && 'rotate-180')}
          />
        </button>

        {aberto && filtradas.length > 0 && (
          <ul
            id={listaId}
            role="listbox"
            aria-label={label ?? 'Sugestões'}
            className="absolute z-50 mt-1 max-h-[220px] w-full overflow-y-auto rounded-ci border border-line-strong bg-elevated py-1 shadow-modal"
          >
            {filtradas.map((opcao, i) => {
              const emDestaque = i === destaque
              const escolhida = normalizar(opcao) === normalizar(value)
              return (
                <li
                  key={opcao}
                  id={`${listaId}-${i}`}
                  role="option"
                  aria-selected={escolhida}
                  // `mousedown`, não `click`: o clique só dispara depois do blur, e até lá o
                  // "clique fora" já teria fechado a lista debaixo do cursor.
                  onMouseDown={(e) => {
                    e.preventDefault()
                    escolher(opcao)
                  }}
                  onMouseEnter={() => setDestaque(i)}
                  className={cn(
                    'cursor-pointer px-3 py-[7px] text-[13px] transition-colors',
                    emDestaque ? 'bg-recess text-ink' : 'text-body',
                    escolhida && 'font-semibold text-ink',
                  )}
                >
                  {opcao}
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </Campo>
  )
}

/** Minúsculo e sem acento — o filtro não pode exigir que o usuário acerte a grafia exata. */
function normalizar(texto: string): string {
  return texto
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
}
