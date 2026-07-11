import { cn } from '@/lib/utils'
import { CLASSES, TOTAL_CLASSES, corDaClasse, ehPlotavel } from '@/domain/enums'
import { useTheme } from '@/theme/ThemeProvider'

export type BarraColormapSize = 'tile' | 'card'

/** Geometria por tamanho — spec 01 §7.7 / 04 §3.2. */
const DIMS: Record<BarraColormapSize, { gap: number; altura: number }> = {
  tile: { gap: 1.5, altura: 6 }, // tile de métrica (tela D) — com marcador triangular
  card: { gap: 2, altura: 8 }, // card do dashboard — sem marcador
}

export interface BarraColormapProps {
  /**
   * Ordem da classe (0..7), direto do backend (`ClasseComplexidade.ordem`).
   * `null` / `undefined` / `-1` (DESCONHECIDO) → barra "fantasma": as 8 células
   * a `opacity:.3`, sem célula ativa e sem marcador. É o estado *calculando* /
   * *sem métrica* — nunca uma barra vazia, nunca uma cor inventada.
   */
  k: number | null | undefined
  /** Default: `true` em `tile`, `false` em `card`. Sem `k` plotável nunca desenha. */
  marcador?: boolean
  size?: BarraColormapSize
  className?: string
}

/**
 * Barra do colormap de complexidade: 8 células (`flex:1`), a única cor do sistema.
 *
 * Regras (§3.2 do índice): `i <= k` opacidade cheia · `i > k` → `.3` ·
 * `i === k` → `box-shadow: 0 0 0 1px ink`. Marcador triangular (7×4px, aponta
 * para baixo) em `left: (k + 0.5) × 12.5%`.
 */
export function BarraColormap({ k, marcador, size = 'tile', className }: BarraColormapProps) {
  const { theme } = useTheme()
  const { gap, altura } = DIMS[size]

  const ativa = ehPlotavel(k) ? k : -1
  const querMarcador = marcador ?? size === 'tile'
  const desenhaMarcador = querMarcador && ativa >= 0
  // O espaço acima é reservado mesmo sem marcador, para a barra fantasma
  // (calculando) não pular de altura quando a classe chega.
  const espaco = querMarcador ? 8 : 0

  return (
    <div className={cn('relative', className)} style={{ paddingTop: espaco }} aria-hidden>
      {desenhaMarcador && (
        <span
          style={{
            position: 'absolute',
            top: 0,
            left: `${(ativa + 0.5) * (100 / TOTAL_CLASSES)}%`,
            transform: 'translateX(-50%)',
            width: 0,
            height: 0,
            borderLeft: '3.5px solid transparent',
            borderRight: '3.5px solid transparent',
            borderTop: '4px solid var(--ink)',
          }}
        />
      )}
      <div className="flex" style={{ gap, height: altura }}>
        {CLASSES.map((classe) => (
          <span
            key={classe.k}
            style={{
              flex: 1,
              background: corDaClasse(classe.k, theme),
              opacity: classe.k > ativa ? 0.3 : 1,
              boxShadow: classe.k === ativa ? '0 0 0 1px var(--ink)' : undefined,
            }}
          />
        ))}
      </div>
    </div>
  )
}
