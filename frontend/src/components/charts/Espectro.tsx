/*
 * ESPECTRO — histograma das resoluções por classe de complexidade.
 *
 * Spec: docs/design/specs/02-graficos.md §4 (+ Lacunas I e J) · 00-INDICE.md §6 item 8.
 * NÃO é SVG: são 8 linhas de flex. A geometria é trivial e o texto (rótulo mono, contagem
 * tabular) fica sendo texto de verdade — selecionável, acessível, sem `<text>`.
 *
 * As 8 CLASSES APARECEM SEMPRE, inclusive as de contagem zero (decisão do contrato):
 * o espectro completo é a leitura correta — a ausência de `O(n!)` diz tanto quanto a
 * presença de `O(n)`. Classe zerada = trilho vazio, sem barra fantasma, contagem `0`
 * em `soft`. O eixo não encolhe com os dados.
 *
 * Cor: só o colormap, e só na barra. Rótulo e contagem são neutros (mono mede).
 */
import { corDaClasse, rotuloCanonico } from '@/domain/enums'
import { cn, pluralPt } from '@/lib/utils'
import { pontoPorId } from './dataset'
import { linhasEspectro } from './escalas'
import type { PropsGrafico } from './tipos'

// ── Geometria (protótipo: card do seletor, spec 02 §4) ──────────────────────
/** Coluna do rótulo: largura fixa para os 8 rótulos alinharem à direita, colados no trilho. */
const ROTULO_LARGURA = 52
/** Coluna da contagem — `min-width` (e não `width`) para 3 dígitos não estourarem. */
const CONTAGEM_LARGURA = 16
/** Altura do trilho (inclui a hairline: `box-sizing: border-box`). */
const TRILHO_ALTURA = 14
/** Espaço entre as 8 linhas. */
const GAP_LINHAS = 8
/** Mono 10.5px em tudo (rótulo e contagem) — spec 02 §4. */
const FONTE = 10.5
/**
 * Barra mínima quando a contagem é > 0. Sem isto, 1 resolução num máximo de 60 daria
 * ~1.7% (≈4px) e, no limite, uma barra invisível — indistinguível do zero. O zero é o
 * único caso SEM barra; nenhuma contagem real pode desaparecer.
 */
const BARRA_MIN = 2

/** Larguras fixas do esqueleto (determinísticas: nada de aleatório em render). */
const LARGURAS_ESQUELETO = [38, 62, 92, 74, 48, 26, 16, 10]

export interface EspectroProps extends PropsGrafico {
  /**
   * Estado de carregamento. `PropsGrafico` não tem este campo (a Carta e as Órbitas
   * também o querem); é opcional e o painel pode simplesmente não passá-lo.
   */
  carregando?: boolean
  className?: string
}

/**
 * Histograma por classe. **Não é interativo** (ver `onSelecionar`, abaixo).
 *
 * `onSelecionar` NÃO é usado: uma barra é uma classe (agregado de N resoluções), não uma
 * resolução — não existe `resolucaoId` para emitir. Filtrar a Carta por classe seria outra
 * feature, não uma interação deste gráfico. Sem clique, sem hover decorativo.
 *
 * `selecionadoId` é usado só de LEITURA: a linha da classe da resolução selecionada em
 * outro gráfico ganha a hairline `ink` (a mesma gramática da célula ativa da BarraColormap)
 * e o rótulo em `ink`. É um eco da seleção, não uma interação.
 */
export function Espectro({
  dataset,
  selecionadoId,
  tema,
  carregando = false,
  className,
}: EspectroProps) {
  const linhas = linhasEspectro(dataset.pontos)
  const kSelecionado = pontoPorId(dataset, selecionadoId)?.k ?? null
  const vazio = !carregando && dataset.pontos.length === 0

  return (
    <div className={cn('flex flex-col', className)}>
      <div
        className="flex flex-col"
        role="list"
        aria-busy={carregando || undefined}
        style={{ gap: GAP_LINHAS, padding: '6px 0' }}
      >
        {linhas.map(({ k, curto, contagem, largura }) => {
          const zerada = contagem === 0
          const selecionada = k === kSelecionado

          return (
            <div
              key={k}
              role="listitem"
              className="flex items-center"
              style={{ gap: 8 }}
              aria-label={`${rotuloCanonico(k)} — ${pluralPt(contagem, 'resolução', 'resoluções')}`}
            >
              {/* Rótulo curto — mono, neutro. A cor da classe é da barra, nunca do texto. */}
              <span
                aria-hidden
                className={cn(
                  'shrink-0 text-right font-mono',
                  selecionada ? 'text-ink' : zerada ? 'text-soft' : 'text-mid',
                )}
                style={{ width: ROTULO_LARGURA, fontSize: FONTE }}
              >
                {curto}
              </span>

              {/* Trilho: hairline sempre visível (regra 7) — é ele que faz a classe
                  ausente EXISTIR na leitura. Fundo `recess` no claro e `panel` no escuro
                  (spec 02 §7: o trilho é um degrau ACIMA do cartão no escuro e ABAIXO no
                  claro; não há um token único que faça as duas coisas). */}
              <div
                aria-hidden
                className={cn(
                  'flex-1 overflow-hidden rounded-ci-sm border bg-recess dark:bg-panel',
                  selecionada ? 'border-ink' : 'border-line-soft',
                )}
                style={{ height: TRILHO_ALTURA }}
              >
                {carregando ? (
                  <div
                    className="ci-pulse h-full bg-elevated"
                    style={{ width: `${LARGURAS_ESQUELETO[k]}%` }}
                  />
                ) : (
                  contagem > 0 && (
                    <div
                      className="h-full"
                      style={{
                        width: `${largura}%`,
                        minWidth: BARRA_MIN,
                        background: corDaClasse(k, tema),
                      }}
                    />
                  )
                )}
              </div>

              {/* Contagem — mono tabular. `0` fica em `soft`: a classe existe, o dado não. */}
              <span
                aria-hidden
                className={cn(
                  'tabular shrink-0 text-right font-mono',
                  zerada || carregando ? 'text-soft' : 'text-mid',
                )}
                style={{ minWidth: CONTAGEM_LARGURA, fontSize: FONTE }}
              >
                {carregando ? '·' : contagem}
              </span>
            </div>
          )
        })}
      </div>

      {/* Vazio: o espectro continua desenhado (8 trilhos), só não há barra nenhuma —
          a forma do gráfico não some junto com os dados (spec 02, Lacuna J). */}
      {vazio && (
        <p className="pt-1 text-center font-mono text-soft" style={{ fontSize: 10 }}>
          {dataset.total > 0
            ? 'nenhuma resolução com métrica de complexidade'
            : 'nenhuma resolução analisada'}
        </p>
      )}
    </div>
  )
}
