/*
 * SELETOR DE GRÁFICO — as 5 visualizações do mesmo dataset (spec 02 §1).
 *
 * Não é um segmented control com trilho: é uma fileira de pílulas independentes
 * (ativa = sólida `ink`; inativas = contornadas). Escala do dashboard, não a do guia:
 * gap 6px · mono 11px · padding 5px 10px · raio 3px · hairline 1px em TODAS (inclusive na
 * ativa, senão as alturas divergem com `box-sizing: border-box`).
 *
 * Semântica: `tablist` / `tab` com roving tabindex (setas, Home/End) — um seletor que só
 * responde ao mouse exclui quem navega por teclado.
 *
 * O estado vive na URL (`?view=carta|orbitas|espectro|linha|matriz`) via `useGraficoNaUrl`:
 * a visualização é compartilhável e sobrevive a um reload ou a um "voltar" do navegador.
 */
import { useCallback, useId, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import { cn } from '@/lib/utils'

export type TipoGrafico = 'carta' | 'orbitas' | 'espectro' | 'linha' | 'matriz'

export interface ItemGrafico {
  chave: TipoGrafico
  rotulo: string
}

/** Ordem canônica (spec 02 §1). Padrão: **Carta**. */
export const GRAFICOS: readonly ItemGrafico[] = [
  { chave: 'carta', rotulo: 'Carta' },
  { chave: 'orbitas', rotulo: 'Órbitas' },
  { chave: 'espectro', rotulo: 'Espectro' },
  { chave: 'linha', rotulo: 'Linha' },
  { chave: 'matriz', rotulo: 'Matriz' },
]

export const GRAFICO_PADRAO: TipoGrafico = 'carta'

/** Nome do query param que carrega a visualização. */
export const PARAM_GRAFICO = 'view'

export function ehTipoGrafico(valor: string | null | undefined): valor is TipoGrafico {
  return !!valor && GRAFICOS.some((g) => g.chave === valor)
}

/**
 * A visualização ativa, guardada na URL. `?view=` ausente ou inválido → `carta`.
 *
 * `replace: true` de propósito: alternar entre 5 gráficos não deve empilhar 5 entradas no
 * histórico (o "voltar" tem de sair da tela, não desfazer cliques). Mesmo assim, a entrada
 * corrente guarda o `?view=` — quem sai para uma resolução e volta reencontra o seu gráfico.
 * O valor padrão não polui a URL (o param só aparece quando difere de `carta`).
 */
export function useGraficoNaUrl(
  param: string = PARAM_GRAFICO,
): [TipoGrafico, (proximo: TipoGrafico) => void] {
  const [searchParams, setSearchParams] = useSearchParams()
  const bruto = searchParams.get(param)
  const atual = ehTipoGrafico(bruto) ? bruto : GRAFICO_PADRAO

  const definir = useCallback(
    (proximo: TipoGrafico) => {
      setSearchParams(
        (anterior) => {
          const seguinte = new URLSearchParams(anterior)
          if (proximo === GRAFICO_PADRAO) seguinte.delete(param)
          else seguinte.set(param, proximo)
          return seguinte
        },
        { replace: true },
      )
    },
    [param, setSearchParams],
  )

  return [atual, definir]
}

export interface SeletorDeGraficoProps {
  value: TipoGrafico
  onChange: (proximo: TipoGrafico) => void
  /**
   * Prefixo dos ids ARIA. Quem renderiza o painel do gráfico passa o mesmo `baseId` e usa
   * `id={`${baseId}-painel`}` + `aria-labelledby={`${baseId}-tab-${value}`}` na área do gráfico.
   */
  baseId?: string
  className?: string
}

export function SeletorDeGrafico({ value, onChange, baseId, className }: SeletorDeGraficoProps) {
  const idInterno = useId()
  const base = baseId ?? idInterno
  const refs = useRef<(HTMLButtonElement | null)[]>([])

  const mover = (de: number, passo: number) => {
    const total = GRAFICOS.length
    const alvo = (de + passo + total) % total
    onChange(GRAFICOS[alvo].chave)
    refs.current[alvo]?.focus()
  }

  const aoTeclar = (evento: React.KeyboardEvent<HTMLButtonElement>, indice: number) => {
    switch (evento.key) {
      case 'ArrowRight':
      case 'ArrowDown':
        evento.preventDefault()
        mover(indice, 1)
        break
      case 'ArrowLeft':
      case 'ArrowUp':
        evento.preventDefault()
        mover(indice, -1)
        break
      case 'Home':
        evento.preventDefault()
        mover(0, 0)
        break
      case 'End':
        evento.preventDefault()
        mover(GRAFICOS.length - 1, 0)
        break
      default:
        break
    }
  }

  return (
    <div
      role="tablist"
      aria-label="Visualização do portfólio"
      aria-orientation="horizontal"
      className={cn('flex flex-wrap items-center gap-1.5', className)}
    >
      {GRAFICOS.map((grafico, indice) => {
        const ativo = grafico.chave === value

        return (
          <button
            key={grafico.chave}
            ref={(el) => {
              refs.current[indice] = el
            }}
            type="button"
            role="tab"
            id={`${base}-tab-${grafico.chave}`}
            aria-selected={ativo}
            aria-controls={`${base}-painel`}
            tabIndex={ativo ? 0 : -1}
            onClick={() => onChange(grafico.chave)}
            onKeyDown={(evento) => aoTeclar(evento, indice)}
            className={cn(
              'ci-foco-botao cursor-pointer rounded-ci border px-2.5 py-[5px] font-mono text-[11px] transition-colors',
              ativo
                ? 'border-ink bg-ink font-semibold text-ink-on'
                : 'border-line-strong bg-transparent text-mid hover:border-ink hover:text-ink',
            )}
          >
            {grafico.rotulo}
          </button>
        )
      })}
    </div>
  )
}
