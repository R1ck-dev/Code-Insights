/*
 * MATRIZ — heatmap densidade (autonomia × classe de complexidade).
 *
 * É a Carta binada: as MESMAS duas variáveis, os MESMOS eixos, a MESMA orientação —
 * só que contando resoluções por célula em vez de plotar estrelas. Quem entendeu a
 * Carta entende a Matriz sem reaprender nada. Spec: docs/design/specs/02-graficos.md §6.
 *
 * Divisão de trabalho: a INTENSIDADE (alpha) diz QUANTAS resoluções há na célula; a
 * MATIZ vem do colormap da classe e é compartilhada pela LINHA inteira. Ou seja: a cor
 * nunca inventa informação nova — ela só repete o eixo Y, que é o próprio colormap.
 *
 * ⚠ CSS Grid, não SVG: o domínio é discreto (5 × 8 = 40 células) e a geometria é uma
 * tabela. SVG só acrescentaria cálculo de coordenada para nada.
 */
import {
  type CSSProperties,
  type KeyboardEvent,
  type MouseEvent,
  useMemo,
  useRef,
  useState,
} from 'react'
import { Folder } from 'lucide-react'

import { EmptyState } from '@/components/ui/empty-state'
import {
  type ClasseK,
  corDaClasse,
  rgbaDeHex,
  rotuloCanonico,
  tintaDaClasse,
} from '@/domain/enums'
import { cn, pluralPt } from '@/lib/utils'

import { pontoPorId } from './dataset'
import { montarMatriz } from './escalas'
import {
  AUTONOMIA_MIN,
  type NivelAutonomia,
  type PontoPlotavel,
  type PropsGrafico,
  TOTAL_AUTONOMIA,
} from './tipos'

// ── Geometria (protótipo, spec 02 §6.1) ─────────────────────────────────────
const ALTURA_CELULA = 30
const GAP = 5
const LARGURA_ROTULO = 52
/** `52px repeat(5, 1fr)` — a coluna de rótulos + as 5 colunas de autonomia. */
const COLUNAS = `${LARGURA_ROTULO}px repeat(${TOTAL_AUTONOMIA}, minmax(0, 1fr))`

/** Níveis de autonomia, 1 → 5 (colunas, da esquerda para a direita). */
const AUTONOMIAS: NivelAutonomia[] = Array.from(
  { length: TOTAL_AUTONOMIA },
  (_, i) => (AUTONOMIA_MIN + i) as NivelAutonomia,
)

/** Metade da largura estimada do callout — usada para não deixá-lo vazar a grade. */
const CALLOUT_MEIA_LARGURA = 96
/** Acima disso o callout ainda cabe em cima da célula; abaixo, ele desce. */
const CALLOUT_ESPACO_ACIMA = 44
const CALLOUT_FOLGA = 8

/** Degraus da rampa da legenda de intensidade (neutros — a matiz é da linha, não da legenda). */
const RAMPA_LEGENDA = [0.16, 0.36, 0.56, 0.76]

interface CelulaAtiva {
  k: ClasseK
  autonomia: NivelAutonomia
  contagem: number
  /** centro horizontal da célula, relativo à grade (já limitado às bordas). */
  x: number
  yTopo: number
  yBase: number
}

export interface MatrizProps extends PropsGrafico {
  /** Esqueleto pulsante no lugar dos dados; a grade e os eixos continuam desenhados. */
  carregando?: boolean
  /** Rodapé com a rampa de intensidade. O painel pode desligar se já tiver o seu. */
  legenda?: boolean
  className?: string
}

/**
 * Matriz de densidade: 5 colunas (autonomia 1→5) × 8 linhas (O(n!) no topo → O(1) embaixo).
 *
 * Componente PURO: recebe o dataset já montado e o tema; não busca dado, não guarda seleção
 * (só a reflete) e não desenha cabeçalho — isso é do painel.
 */
export function Matriz({
  dataset,
  selecionadoId,
  onSelecionar,
  tema,
  carregando = false,
  legenda = true,
  className,
}: MatrizProps) {
  const gradeRef = useRef<HTMLDivElement>(null)
  const [hover, setHover] = useState<CelulaAtiva | null>(null)

  const matriz = useMemo(() => montarMatriz(dataset.pontos), [dataset.pontos])

  /*
   * ⚠ ORIENTAÇÃO — NÃO "CORRIJA" ISTO.
   * `montarMatriz` devolve as linhas em k = 0..7 (O(1) primeiro), seguindo o markup antigo do
   * protótipo, que punha o mais eficiente no topo. A decisão de produto vigente é a INVERSA:
   * a Matriz usa a MESMA orientação da Carta — O(n!) no topo, O(1) na base — porque as duas
   * são projeções do mesmo par de eixos e ler uma invertida em relação à outra faz o aluno
   * concluir o oposto do que o dado diz. Coerência entre visualizações é obrigatória.
   *
   * Inverto aqui, na hora de desenhar, e não em `escalas.ts` (que é compartilhado e cuja
   * ordem k=0..7 preserva o invariante "soma da linha == contagem da classe no Espectro").
   */
  const linhas = useMemo(() => [...matriz.linhas].reverse(), [matriz])

  /*
   * Índice célula → pontos. `montarMatriz` só conta; para SELECIONAR uma célula é preciso o
   * `resolucaoId`, e ele só existe aqui. Chave: `autonomia:k`.
   */
  const pontosPorCelula = useMemo(() => {
    const mapa = new Map<string, PontoPlotavel[]>()
    for (const p of dataset.pontos) {
      const chave = `${p.autonomia}:${p.k}`
      const grupo = mapa.get(chave)
      if (grupo) grupo.push(p)
      else mapa.set(chave, [p])
    }
    return mapa
  }, [dataset.pontos])

  /** A célula do ponto selecionado (vem do painel) — ganha o anel permanente. */
  const selecionado = pontoPorId(dataset, selecionadoId)

  const vazio = !carregando && dataset.pontos.length === 0

  function aoEntrar(
    e: MouseEvent<HTMLDivElement>,
    k: ClasseK,
    autonomia: NivelAutonomia,
    contagem: number,
  ) {
    if (carregando) return
    const celula = e.currentTarget
    const largura = gradeRef.current?.clientWidth ?? 0
    const centro = celula.offsetLeft + celula.offsetWidth / 2
    // Limita o centro do callout para que ele não vaze pela esquerda nem pela direita.
    const min = Math.min(CALLOUT_MEIA_LARGURA, largura / 2)
    const x = Math.min(Math.max(centro, min), Math.max(largura - min, min))
    setHover({
      k,
      autonomia,
      contagem,
      x,
      yTopo: celula.offsetTop,
      yBase: celula.offsetTop + celula.offsetHeight,
    })
  }

  /**
   * Seleção só com contagem EXATAMENTE 1: aí a célula é uma resolução e clicar tem um
   * significado único. Com 2+ não há o que selecionar sem escolher por conta própria —
   * e escolher pelo usuário seria inventar dado. (A célula continua com hover e tooltip.)
   */
  function idSelecionavel(k: ClasseK, autonomia: NivelAutonomia, contagem: number): string | null {
    if (contagem !== 1 || !onSelecionar) return null
    return pontosPorCelula.get(`${autonomia}:${k}`)?.[0]?.resolucaoId ?? null
  }

  return (
    <div className={cn('flex flex-col gap-2.5', className)}>
      <div className="relative">
        {/* ── A grade ───────────────────────────────────────────────────── */}
        <div
          ref={gradeRef}
          role="grid"
          aria-label={`Matriz de densidade: autonomia (1 a 5) por classe de complexidade. ${pluralPt(
            dataset.pontos.length,
            'resolução plotada',
            'resoluções plotadas',
          )}.`}
          className={cn('relative grid', vazio && 'pointer-events-none opacity-40')}
          style={{ gridTemplateColumns: COLUNAS, gap: GAP }}
          onMouseLeave={() => setHover(null)}
        >
          {linhas.map((linha) => {
            const cor = corDaClasse(linha.k, tema)
            return (
              // `display: contents` mantém as células como filhas diretas da grade (a
              // geometria continua sendo a da grade) e ainda dá semântica de linha.
              <div key={linha.k} role="row" style={{ display: 'contents' }}>
                <span
                  role="rowheader"
                  className="flex items-center justify-end pr-[7px] font-mono text-[10px] text-mid"
                  style={{ height: ALTURA_CELULA }}
                >
                  {linha.curto}
                </span>

                {linha.celulas.map((celula) => {
                  const temDado = celula.contagem > 0
                  const idParaSelecionar = idSelecionavel(linha.k, celula.autonomia, celula.contagem)
                  const estaSelecionada =
                    !!selecionado &&
                    selecionado.k === linha.k &&
                    selecionado.autonomia === celula.autonomia
                  const emHover =
                    !!hover && hover.k === linha.k && hover.autonomia === celula.autonomia
                  const realce = (estaSelecionada || emHover) && !carregando

                  /*
                   * Substrato NEUTRO em toda célula (`graf-grade`) + a matiz da classe por
                   * cima, só quando há dado. Assim uma célula vazia nunca é um quadrado
                   * colorido: ela é grade. E a cor translúcida compõe sobre a grade, não
                   * sobre o fundo do cartão — a rampa fica estável em qualquer contagem.
                   */
                  const estilo: CSSProperties = {
                    height: ALTURA_CELULA,
                    backgroundColor: 'var(--graf-grade)',
                    backgroundImage:
                      temDado && !carregando
                        ? `linear-gradient(${rgbaDeHex(cor, celula.alpha)}, ${rgbaDeHex(cor, celula.alpha)})`
                        : undefined,
                    boxShadow: realce ? 'inset 0 0 0 1px var(--ink)' : undefined,
                  }

                  return (
                    <div
                      key={celula.autonomia}
                      role="gridcell"
                      aria-label={rotuloCelula(celula.contagem, celula.autonomia, linha.k)}
                      aria-selected={estaSelecionada || undefined}
                      tabIndex={idParaSelecionar ? 0 : -1}
                      className={cn(
                        'ci-foco-botao flex select-none items-center justify-center rounded-ci-sm font-mono text-[11px] tabular',
                        'motion-safe:transition-shadow motion-safe:duration-100',
                        carregando && 'ci-pulse',
                        idParaSelecionar ? 'cursor-pointer' : 'cursor-default',
                        temDado ? 'text-ink' : 'text-graf-vazio',
                      )}
                      style={estilo}
                      onMouseEnter={(e) => aoEntrar(e, linha.k, celula.autonomia, celula.contagem)}
                      onFocus={() => setHover(null)}
                      onClick={
                        idParaSelecionar ? () => onSelecionar?.(idParaSelecionar) : undefined
                      }
                      onKeyDown={
                        idParaSelecionar
                          ? (e: KeyboardEvent<HTMLDivElement>) => {
                              if (e.key !== 'Enter' && e.key !== ' ') return
                              e.preventDefault()
                              onSelecionar?.(idParaSelecionar)
                            }
                          : undefined
                      }
                    >
                      {/* Vazio é `·`, nunca `0`: ausência de resolução não é uma medida. */}
                      {carregando ? '' : temDado ? celula.contagem : '·'}
                    </div>
                  )
                })}
              </div>
            )
          })}
        </div>

        {/* ── Callout (mono, sobre a grade) ─────────────────────────────── */}
        {hover && !vazio && !carregando && (
          <div
            className="pointer-events-none absolute z-10 flex flex-col gap-[3px] whitespace-nowrap rounded-ci border bg-elevated px-2.5 py-2 shadow-callout"
            style={{
              left: hover.x,
              top:
                hover.yTopo < CALLOUT_ESPACO_ACIMA
                  ? hover.yBase + CALLOUT_FOLGA
                  : hover.yTopo - CALLOUT_FOLGA,
              transform: `translate(-50%, ${hover.yTopo < CALLOUT_ESPACO_ACIMA ? '0' : '-100%'})`,
              borderColor:
                hover.contagem > 0 ? corDaClasse(hover.k, tema) : 'var(--line)',
            }}
          >
            <span className="font-mono text-[11px] font-semibold text-ink tabular">
              {contagemPorExtenso(hover.contagem)}
            </span>
            <span
              className="font-mono text-[10px] tabular"
              style={{
                color: hover.contagem > 0 ? tintaDaClasse(hover.k, tema) : 'var(--soft)',
              }}
            >
              autonomia {hover.autonomia} · {rotuloCanonico(hover.k)}
            </span>
          </div>
        )}

        {/* ── Vazio: a grade fica (esmaecida) e o bloco vazio entra por cima ─ */}
        {vazio && (
          <div className="absolute inset-0 flex items-center justify-center">
            <EmptyState
              size="sm"
              icon={Folder}
              title="Nenhuma resolução analisada ainda."
              description="Submeta uma resolução em Java para ver a matriz."
            />
          </div>
        )}
      </div>

      {/* ── Eixo X: 1..5 sob as colunas + título, como na Carta (spec 02 §2.10) ── */}
      <div className="grid" style={{ gridTemplateColumns: COLUNAS, gap: GAP }}>
        <span aria-hidden />
        {AUTONOMIAS.map((a) => (
          <span key={a} className="text-center font-mono text-[10px] text-mid tabular">
            {a}
          </span>
        ))}
      </div>
      <div
        aria-hidden
        className="text-center font-mono text-[10px] tracking-[.1em] text-mid"
        style={{ paddingLeft: LARGURA_ROTULO + GAP }}
      >
        AUTONOMIA IA →
      </div>

      {/* ── Rodapé: o que é a intensidade ─────────────────────────────────── */}
      {legenda && (
        <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1.5 pt-0.5 font-mono text-[9px] text-soft">
          <span>colunas = autonomia (1–5) · intensidade = nº de resoluções</span>
          {matriz.maxContagem > 0 && (
            <span className="flex items-center gap-1.5" aria-hidden>
              <span className="text-graf-vazio">·</span>
              <span className="flex items-center gap-[2px]">
                {RAMPA_LEGENDA.map((alpha) => (
                  <span
                    key={alpha}
                    className="rounded-ci-sm"
                    style={{
                      width: 8,
                      height: 8,
                      backgroundColor: 'var(--ink)',
                      opacity: alpha,
                    }}
                  />
                ))}
              </span>
              <span className="tabular">
                {matriz.maxContagem <= 1 ? '1' : `1 → ${matriz.maxContagem}`}
              </span>
            </span>
          )}
        </div>
      )}
    </div>
  )
}

/** "nenhuma resolução" · "1 resolução" · "3 resoluções". Zero não é `0 resoluções`. */
function contagemPorExtenso(contagem: number): string {
  if (contagem <= 0) return 'nenhuma resolução'
  return pluralPt(contagem, 'resolução', 'resoluções')
}

/** Texto único do tooltip e do `aria-label`: o que se lê é o que o leitor de tela ouve. */
function rotuloCelula(contagem: number, autonomia: NivelAutonomia, k: ClasseK): string {
  return `${contagemPorExtenso(contagem)} · autonomia ${autonomia} · ${rotuloCanonico(k)}`
}
