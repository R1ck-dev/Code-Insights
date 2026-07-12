import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'
import { BarraColormap } from '@/components/BarraColormap'
import { ConfidenceChip } from '@/components/domain/badges'
import { type Confianca, comPrefixoEstimado, ehPlotavel, tintaDaClasse } from '@/domain/enums'
import { useTheme } from '@/theme/ThemeProvider'

export interface MetricTileProps {
  /** Rótulo curto, MAIÚSCULO: `CICLOMÁTICA` · `TEMPO` · `ESPAÇO`. */
  rotulo: string
  /** Método da medição: `McCabe` · `Big O · AST` (nunca "heurística"). */
  metodo: string
  /**
   * Valor CRU, sem o `≈` (o tile prefixa quando `confianca === 'ESTIMADO'`):
   * `M = 4`, `O(n²)`, `O(1)`. `null`/`undefined` → `—` em `soft` ("sem métrica":
   * linguagem ≠ Java ou classe DESCONHECIDO).
   */
  valor: string | number | null | undefined
  confianca: Confianca
  /**
   * Ordem da classe (0..7) do backend. Só existe em métricas Big-O — a
   * ciclomática é uma contagem exata, NÃO uma classe, e por isso não tem barra.
   */
  k?: number | null
  /** Rodapé: `2 for + 1 if → P=3 · M = P + 1`. */
  nota?: ReactNode
  /**
   * Força a barra do colormap. Default: só quando há classe (`k` plotável).
   * Passe `barra` nos tiles Big-O em estado `calculando` para manter a barra
   * fantasma (8 células a .3, sem marcador) e a altura do tile.
   */
  barra?: boolean
  /** Análise ainda rodando: o valor vira spinner + `calculando`. */
  calculando?: boolean
  /** Falha na análise: borda e valor em erro. */
  erro?: boolean
  /** Slot à direita do chip de confiança — normalmente o botão `?` (InfoButton). */
  info?: ReactNode
  className?: string
}

/**
 * ★ Tile de leitura de métrica — o componente-assinatura do produto (tela D).
 *
 * Valor mono 31/600: `ink` quando MEDIDO, cor da classe quando ESTIMADO.
 * A incerteza aparece três vezes e nunca é escondida (regra 3): no chip
 * (quadrado vazado), no prefixo `≈` do valor e na cor.
 */
export function MetricTile({
  rotulo,
  metodo,
  valor,
  confianca,
  k,
  nota,
  barra,
  calculando,
  erro,
  info,
  className,
}: MetricTileProps) {
  const { theme } = useTheme()
  const temValor = valor !== null && valor !== undefined && valor !== ''
  const temClasse = ehPlotavel(k)
  const mostraBarra = barra ?? temClasse

  const corDoValor = erro
    ? 'var(--erro-texto)'
    : !temValor
      ? 'var(--soft)'
      : confianca === 'ESTIMADO' && temClasse
        ? tintaDaClasse(k, theme)
        : 'var(--ink)'

  return (
    <div
      className={cn(
        'flex flex-col gap-2.5 rounded-ci border bg-recess px-[15px] py-3.5',
        erro ? 'border-erro-line' : 'border-line',
        className,
      )}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex flex-col gap-0.5">
          <span className="font-mono text-[10.5px] uppercase tracking-[.06em] text-mid">
            {rotulo}
          </span>
          <span className="font-mono text-[9px] text-soft">{metodo}</span>
        </div>
        <div className="flex shrink-0 items-center gap-1.5">
          <ConfidenceChip tipo={confianca} compact />
          {info}
        </div>
      </div>

      {calculando ? (
        <span className="inline-flex items-center gap-2 font-mono text-[11px] text-ink">
          <span
            aria-hidden
            className="ci-spin-chip inline-block shrink-0 rounded-full"
            style={{ width: 9, height: 9, border: '1.5px solid currentColor', borderTopColor: 'transparent' }}
          />
          calculando
        </span>
      ) : (
        <span
          className="font-mono text-[31px] font-semibold leading-none tabular-nums"
          style={{ color: corDoValor }}
        >
          {temValor ? comPrefixoEstimado(String(valor), confianca) : '—'}
        </span>
      )}

      {mostraBarra && <BarraColormap k={calculando ? null : k} size="tile" />}

      {nota && (
        <p className="font-mono text-[10.5px] leading-[1.45] text-soft">{nota}</p>
      )}
    </div>
  )
}
