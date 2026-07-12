/*
 * SELETOR DE GRÁFICO — as 4 visualizações do mesmo dataset (spec 02 §1).
 *
 * Não é um segmented control com trilho: é uma fileira de pílulas independentes
 * (ativa = sólida `ink`; inativas = contornadas). Escala do dashboard, não a do guia:
 * gap 6px · mono 11px · padding 5px 10px · raio 3px · hairline 1px em TODAS (inclusive na
 * ativa, senão as alturas divergem com `box-sizing: border-box`).
 *
 * Semântica: `tablist` / `tab` com roving tabindex (setas, Home/End) — um seletor que só
 * responde ao mouse exclui quem navega por teclado.
 *
 * O estado vive na URL (`?view=carta|orbitas|linha|matriz`) via `useGraficoNaUrl`: a
 * visualização é compartilhável e sobrevive a um reload ou a um "voltar" do navegador.
 *
 * ⚠ O ESPECTRO SAIU (decisão do usuário, rodada de correção). Ele era a única das 5 vistas que
 * não plotava resoluções — plotava CLASSES —, não tinha o que selecionar, e o dashboard já
 * mostra a mesma distribuição no card "Distribuição · Espectro", ao lado do painel. Duas vezes
 * o mesmo histograma na mesma tela é ruído, não redundância útil. O histograma em si NÃO
 * morreu: `linhasEspectro()` (escalas.ts) continua sendo a fonte daquele card.
 */
import { useCallback, useEffect, useId, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import { cn } from '@/lib/utils'

export type TipoGrafico = 'carta' | 'orbitas' | 'linha' | 'matriz'

export interface ItemGrafico {
  chave: TipoGrafico
  rotulo: string
}

/**
 * Ordem canônica (spec 02 §1). Padrão: **Carta**.
 *
 * ⚠ A CHAVE `orbitas` fica — é o valor que já circula em `?view=` e em links salvos. O que muda
 * é só o RÓTULO: o gráfico foi redesenhado (raio = tempo, tamanho = autonomia, cor = classe) e
 * "Órbitas" descrevia a geometria antiga — anéis concêntricos de autonomia. Hoje ele é uma
 * espiral do tempo, e o próprio gráfico se apresenta assim no seu cabeçalho interno. Rótulo e
 * desenho voltam a dizer a mesma coisa; nome de arquivo/export não são texto de interface.
 */
export const GRAFICOS: readonly ItemGrafico[] = [
  { chave: 'carta', rotulo: 'Carta' },
  { chave: 'orbitas', rotulo: 'Espiral' },
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
 * `replace: true` de propósito: alternar entre os gráficos não deve empilhar uma entrada por
 * clique no histórico (o "voltar" tem de sair da tela, não desfazer cliques). Mesmo assim, a
 * entrada corrente guarda o `?view=` — quem sai para uma resolução e volta reencontra o seu
 * gráfico. O valor padrão não polui a URL (o param só aparece quando difere de `carta`).
 *
 * ⚠ LINK ANTIGO (`?view=espectro`): a visualização não existe mais. Ele já cai na Carta pelo
 * fallback acima — mas o param SOBREVIVERIA na barra de endereços, mentindo sobre o que está
 * na tela e sendo repassado adiante num compartilhamento. O efeito abaixo o apaga (em
 * `replace`, sem entrada de histórico). Vale para qualquer valor inválido, não só o espectro.
 */
export function useGraficoNaUrl(
  param: string = PARAM_GRAFICO,
): [TipoGrafico, (proximo: TipoGrafico) => void] {
  const [searchParams, setSearchParams] = useSearchParams()
  const bruto = searchParams.get(param)
  const valido = ehTipoGrafico(bruto)
  const atual = valido ? bruto : GRAFICO_PADRAO

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

  useEffect(() => {
    // `definir(GRAFICO_PADRAO)` = deletar o param. `bruto` vira `null` e o efeito não repete.
    if (bruto != null && !valido) definir(GRAFICO_PADRAO)
  }, [bruto, valido, definir])

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
