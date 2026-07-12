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
import { type CSSProperties, type MouseEvent, useMemo, useRef, useState } from 'react'
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
    e: MouseEvent<HTMLButtonElement>,
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
   * QUALQUER célula com resolução é clicável — inclusive as agrupadas (correção pedida pelo
   * usuário; antes, só contagem === 1 abria).
   *
   * O argumento antigo era "com 2+ não há o que selecionar sem escolher pelo usuário". Só que o
   * efeito prático foi pior do que o problema: a célula com 3 resoluções — justamente a mais
   * densa, a que o aluno mais quer abrir — não tinha porta nenhuma. O dado existia e era
   * inalcançável pelo gráfico.
   *
   * A saída é a MESMA da Carta (que agrupa pelo mesmo critério — a Matriz é a Carta binada):
   * clicar abre a MAIS ANTIGA do grupo, e as irmãs saem pelas setas "‹ 2 de 3 ›" do painel
   * lateral. Não é escolher em silêncio: o tooltip diz o que o clique faz antes de ele acontecer.
   *
   * ⚠ `dataset.pontos` está em ordem cronológica ASC (contrato de `montarDataset`) e
   * `pontosPorCelula` preserva essa ordem — logo `[0]` é a mais antiga.
   */
  function idSelecionavel(k: ClasseK, autonomia: NivelAutonomia, contagem: number): string | null {
    if (contagem < 1 || !onSelecionar) return null
    return pontosPorCelula.get(`${autonomia}:${k}`)?.[0]?.resolucaoId ?? null
  }

  return (
    <div className={cn('flex h-full flex-col gap-2.5', className)}>
      {/*
       * ⚠ A grade CRESCE com o cartão (correção do "espaço estranho em cima e embaixo"). A Matriz
       * é CSS Grid, não SVG: ela não tem razão de aspecto a preservar, então pode simplesmente
       * ocupar a altura que sobra em vez de deixá-la como ar. As linhas nunca encolhem abaixo de
       * `ALTURA_CELULA` (o piso do protótipo) — `minmax(ALTURA_CELULA, 1fr)`.
       */}
      <div className="relative flex-1">
        {/* ── A grade ───────────────────────────────────────────────────── */}
        <div
          ref={gradeRef}
          role="grid"
          aria-label={`Matriz de densidade: autonomia (1 a 5) por classe de complexidade. ${pluralPt(
            dataset.pontos.length,
            'resolução plotada',
            'resoluções plotadas',
          )}.`}
          className={cn('relative grid h-full', vazio && 'pointer-events-none opacity-40')}
          style={{
            gridTemplateColumns: COLUNAS,
            gridAutoRows: `minmax(${ALTURA_CELULA}px, 1fr)`,
            gap: GAP,
          }}
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
                  className="flex h-full items-center justify-end pr-[7px] font-mono text-[10px] text-mid"
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
                    // A altura vem da LINHA da grade (`gridAutoRows`), não daqui: assim a célula
                    // acompanha o cartão em vez de fixar 30px e deixar o resto como ar.
                    height: '100%',
                    backgroundColor: 'var(--graf-grade)',
                    backgroundImage:
                      temDado && !carregando
                        ? `linear-gradient(${rgbaDeHex(cor, celula.alpha)}, ${rgbaDeHex(cor, celula.alpha)})`
                        : undefined,
                  }

                  /*
                   * A célula é um <button> DE VERDADE (não um div com onKeyDown): teclado,
                   * leitor de tela e clique saem de graça. As não-selecionáveis continuam
                   * focáveis — quem navega por teclado tem de conseguir LER as 40 contagens,
                   * não só a única que dá para clicar.
                   */
                  return (
                    <div
                      key={celula.autonomia}
                      role="gridcell"
                      aria-selected={estaSelecionada || undefined}
                      className="contents"
                    >
                      <button
                        type="button"
                        // `data-realce` em vez de box-shadow inline: um style inline venceria
                        // o anel de `:focus-visible` e a célula focada ficaria sem foco visível.
                        data-realce={realce || undefined}
                        aria-label={rotuloCelula(
                          celula.contagem,
                          celula.autonomia,
                          linha.k,
                          !!idParaSelecionar,
                        )}
                        aria-pressed={idParaSelecionar ? estaSelecionada : undefined}
                        disabled={carregando}
                        className={cn(
                          'ci-foco-botao ci-celula-matriz flex w-full select-none items-center justify-center rounded-ci-sm font-mono text-[11px] tabular',
                          'motion-safe:transition-shadow motion-safe:duration-100',
                          carregando && 'ci-pulse',
                          idParaSelecionar ? 'cursor-pointer' : 'cursor-default',
                          temDado ? 'text-ink' : 'text-graf-vazio',
                        )}
                        style={estilo}
                        onMouseEnter={(e) =>
                          aoEntrar(e, linha.k, celula.autonomia, celula.contagem)
                        }
                        onFocus={() => setHover(null)}
                        onClick={
                          idParaSelecionar ? () => onSelecionar?.(idParaSelecionar) : undefined
                        }
                      >
                        {/* Vazio é `·`, nunca `0`: ausência de resolução não é uma medida. */}
                        {carregando ? '' : temDado ? celula.contagem : '·'}
                      </button>
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
            {/*
             * Regra 4 (inviolável): AUTONOMIA É NEUTRA. Pintar "autonomia 4" com a tinta da
             * classe diria que a autonomia é laranja porque a solução é O(n²) — a associação
             * que este sistema existe para impedir. Só a CLASSE veste o colormap.
             */}
            <span className="flex items-center gap-1 font-mono text-[10px] tabular">
              <span className="text-ink">autonomia {hover.autonomia}</span>
              <span className="text-soft">·</span>
              <span
                style={{
                  color: hover.contagem > 0 ? tintaDaClasse(hover.k, tema) : 'var(--soft)',
                }}
              >
                {rotuloCanonico(hover.k)}
              </span>
            </span>
            {/* Célula agrupada: o clique escolhe uma das N — e diz qual, antes de escolher. */}
            {hover.contagem > 1 && onSelecionar && (
              <span className="font-mono text-[9.5px] text-soft">
                clique para abrir a mais antiga
              </span>
            )}
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
function rotuloCelula(
  contagem: number,
  autonomia: NivelAutonomia,
  k: ClasseK,
  clicavel: boolean,
): string {
  const base = `${contagemPorExtenso(contagem)} · autonomia ${autonomia} · ${rotuloCanonico(k)}`
  if (!clicavel) return base
  // O que o clique faz, dito antes de acontecer: com 2+ ele escolhe uma das resoluções da célula.
  return contagem > 1 ? `${base}. Abrir a mais antiga.` : `${base}. Abrir.`
}
