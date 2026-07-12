/*
 * CARTA CELESTE — o gráfico-assinatura do Órbita.
 *
 * Um scatter que se lê como um céu: X = Índice de Autonomia IA (1→5), Y = classe de
 * complexidade (O(1) EMBAIXO → O(n!) NO TOPO — quanto mais alto, mais custoso). A cor do
 * ponto é a classe (o colormap é a ÚNICA cor do sistema) e o eixo Y É o próprio colormap.
 *
 * A metáfora central são as CONSTELAÇÕES: uma polyline por desafio, ligando suas resoluções
 * em ordem cronológica. É a trajetória do aluno naquele desafio — força bruta com muito apoio
 * de IA → solução refinada e autônoma. É exatamente o dado que a decisão D2 (nova resolução
 * no lugar de editar) produz, e o que a pesquisa mede.
 *
 * Componente PURO: recebe o dataset já montado (`./dataset`), a geometria vem de `./escalas`
 * e as cores de `@/domain/enums`. Não busca dado, não guarda seleção — só desenha e avisa.
 *
 * Specs: docs/design/specs/02-graficos.md §2 (geometria extraída do protótipo) · 00-INDICE
 * §2.7 (regras invioláveis) · §6-A Lacuna 11 (constelações).
 */
import { useMemo, useState } from 'react'
import { Folder } from 'lucide-react'
import {
  CONFIANCA_BIG_O,
  LINGUAGEM_META,
  NOTA_METRICAS_SO_JAVA,
  comPrefixoEstimado,
  corDaClasse,
  rotuloCanonico,
  rotuloConfiancaMotor,
  rotuloCurto,
  tintaDaClasse,
} from '@/domain/enums'
import { cn, pluralPt } from '@/lib/utils'
import {
  CARTA_CELULA_O1,
  CARTA_EIXO_Y,
  CARTA_GRADE_X_FIM,
  CARTA_PLOT,
  CARTA_TICKS_Y,
  CARTA_TICK_X_Y,
  CARTA_TICK_Y_X,
  CARTA_TITULO_X,
  CARTA_VIEWBOX,
  CARTA_X,
  type Ponto2D,
  celulasEixoY,
  dataCompleta,
  deslocamentosCoincidentes,
  posicaoCallout,
  posicaoNaCarta,
  xDaAutonomia,
  yDaClasse,
} from './escalas'
import type { PontoPlotavel, PropsGrafico } from './tipos'

export interface CartaProps extends PropsGrafico {
  /** Enquanto a query não resolve: grade + eixo + estrelas-fantasma pulsando. */
  carregando?: boolean
  className?: string
}

// ── Marcas (spec 02 §2.8) ───────────────────────────────────────────────────
const HALO_R = 7
const HALO_R_HOVER = 9
const NUCLEO_R = 2.6
const SEL_HALO_R = 13
const SEL_ANEL_R = 8
const SEL_NUCLEO_R = 3.4
/** Braços da cruzeta da estrela selecionada: 129.1 ± 16.1 (conferido no markup). */
const CRUZETA_BRACO = 16.1
/** Alvo de clique invisível — o núcleo de 2.6px é pequeno demais para mouse e toque. */
const ALVO_R = 12
const FOCO_R = 11.5

/**
 * Céu decorativo (spec 02 §2.6): 9 estrelas cenográficas FIXAS. Não são dado — são
 * `aria-hidden` e não recebem eventos. No claro viram pontos de tinta (r +0.4, opacidade +.1).
 */
const CEU: readonly { cx: number; cy: number; r: number; o: number }[] = [
  { cx: 120, cy: 60, r: 0.9, o: 0.7 },
  { cx: 250, cy: 45, r: 0.7, o: 0.5 },
  { cx: 390, cy: 70, r: 1.0, o: 0.8 },
  { cx: 500, cy: 50, r: 0.7, o: 0.5 },
  { cx: 210, cy: 160, r: 0.7, o: 0.45 },
  { cx: 470, cy: 150, r: 0.9, o: 0.6 },
  { cx: 95, cy: 120, r: 0.7, o: 0.5 },
  { cx: 345, cy: 180, r: 0.7, o: 0.45 },
  { cx: 520, cy: 200, r: 0.8, o: 0.55 },
]

/** Estrelas-fantasma do estado *carregando* (autonomia, k) — posições fixas, sem dado. */
const FANTASMAS: readonly [number, number][] = [
  [2, 1],
  [3, 2],
  [1, 2],
  [4, 3],
  [3, 5],
  [5, 4],
  [4, 2],
]

/** Rótulo do eixo X (1..5) — a escala de autonomia é discreta e completa. */
const TICKS_X = [1, 2, 3, 4, 5] as const

export function Carta({
  dataset,
  selecionadoId,
  onSelecionar,
  tema,
  carregando = false,
  className,
}: CartaProps) {
  const [hoverId, setHoverId] = useState<string | null>(null)
  const [focoId, setFocoId] = useState<string | null>(null)

  const escuro = tema === 'dark'
  const { pontos, constelacoes } = dataset
  const clicavel = typeof onSelecionar === 'function'

  /**
   * O domínio da Carta é DISCRETO (5 autonomias × 8 classes = 40 posições): duas resoluções
   * na mesma célula cairiam no mesmo pixel e uma esconderia a outra — apagando dado. O jitter
   * de `escalas` abre um anel mínimo em torno da posição real. As constelações usam ESTAS
   * posições (e não as ideais), senão a linha não encostaria nas estrelas.
   */
  const posicoes = useMemo(() => {
    const deslocamentos = deslocamentosCoincidentes(pontos.map((p) => `${p.autonomia}:${p.k}`))
    const mapa = new Map<string, Ponto2D>()
    pontos.forEach((p, i) => {
      const base = posicaoNaCarta(p)
      mapa.set(p.resolucaoId, {
        x: base.x + deslocamentos[i].dx,
        y: base.y + deslocamentos[i].dy,
      })
    })
    return mapa
  }, [pontos])

  // Hover e foco de teclado revelam a mesma informação (paridade: um scatter que só responde
  // ao mouse exclui quem navega por teclado). A seleção é o estado persistente.
  const destacadoId = hoverId ?? focoId ?? selecionadoId ?? null
  const destacado = destacadoId ? (pontos.find((p) => p.resolucaoId === destacadoId) ?? null) : null
  const selecionado = selecionadoId
    ? (pontos.find((p) => p.resolucaoId === selecionadoId) ?? null)
    : null

  const vazio = !carregando && pontos.length === 0

  return (
    <div className={cn('relative w-full', className)}>
      <svg
        viewBox={`0 0 ${CARTA_VIEWBOX.largura} ${CARTA_VIEWBOX.altura}`}
        width="100%"
        className="block h-auto w-full"
        role="group"
        aria-label="Carta de resoluções: autonomia de IA (1 a 5) no eixo horizontal, classe de complexidade no eixo vertical."
      >
        {/* ── Céu decorativo ─────────────────────────────────────────────── */}
        <g aria-hidden className="pointer-events-none">
          {CEU.map((e, i) => (
            <circle
              key={i}
              cx={e.cx}
              cy={e.cy}
              r={escuro ? e.r : e.r + 0.4}
              fill="var(--ink)"
              opacity={escuro ? e.o : Math.min(1, e.o + 0.1)}
            />
          ))}
        </g>

        {/* ── Grade, eixos e o eixo Y = colormap ─────────────────────────── */}
        <g aria-hidden className="pointer-events-none">
          {/* 7 linhas horizontais (k = 1..7) */}
          {[1, 2, 3, 4, 5, 6, 7].map((k) => (
            <line
              key={`h${k}`}
              x1={CARTA_PLOT.x0}
              x2={CARTA_GRADE_X_FIM}
              y1={yDaClasse(k)}
              y2={yDaClasse(k)}
              stroke="var(--graf-grade)"
            />
          ))}
          {/* 4 verticais (autonomia 2..5) */}
          {CARTA_X.slice(1).map((x) => (
            <line
              key={`v${x}`}
              x1={x}
              x2={x}
              y1={CARTA_PLOT.y0}
              y2={CARTA_PLOT.y1}
              stroke="var(--graf-grade)"
            />
          ))}
          {/* linha de base (k = 0) e eixo Y */}
          <line
            x1={CARTA_PLOT.x0}
            x2={CARTA_GRADE_X_FIM}
            y1={CARTA_PLOT.y1}
            y2={CARTA_PLOT.y1}
            stroke="var(--graf-eixo)"
          />
          <line
            x1={CARTA_PLOT.x0}
            x2={CARTA_PLOT.x0}
            y1={CARTA_PLOT.y0}
            y2={CARTA_PLOT.y1}
            stroke="var(--graf-eixo)"
          />

          {/* O eixo Y É a barra de colormap: uma célula por classe (k = 1..7)… */}
          {celulasEixoY().map((c) => (
            <rect
              key={`cx${c.k}`}
              x={CARTA_EIXO_Y.x}
              y={c.y}
              width={CARTA_EIXO_Y.largura}
              height={c.altura}
              fill={corDaClasse(c.k, tema)}
            />
          ))}
          {/* …e o "piso" da escala: O(1) não tem faixa (cairia abaixo da base) — sem este
              quadrado o verde nunca apareceria no eixo (spec 02, Lacuna C). */}
          <rect
            x={CARTA_CELULA_O1.x}
            y={CARTA_CELULA_O1.y}
            width={CARTA_CELULA_O1.lado}
            height={CARTA_CELULA_O1.lado}
            fill={corDaClasse(CARTA_CELULA_O1.k, tema)}
          />

          {/* Ticks do eixo Y (k = 0, 2, 4, 7): sem eles ninguém sabe qual cor é qual classe. */}
          {CARTA_TICKS_Y.map((k) => (
            <text
              key={`ty${k}`}
              x={CARTA_TICK_Y_X}
              y={yDaClasse(k) + 3}
              textAnchor="end"
              fontSize={9}
              fill="var(--soft)"
              className="font-mono"
            >
              {rotuloCurto(k)}
            </text>
          ))}

          {/* Eixo X: autonomia 1..5 + título. */}
          {TICKS_X.map((a) => (
            <text
              key={`tx${a}`}
              x={xDaAutonomia(a)}
              y={CARTA_TICK_X_Y}
              textAnchor="middle"
              fontSize={11}
              fill="var(--soft)"
              className="font-mono tabular"
            >
              {a}
            </text>
          ))}
          <text
            x={CARTA_TITULO_X.x}
            y={CARTA_TITULO_X.y}
            textAnchor="middle"
            fontSize={11}
            letterSpacing={1}
            fill="var(--mid)"
            className="font-mono"
          >
            AUTONOMIA IA →
          </text>
        </g>

        {/* ── Constelações: a trajetória do aluno em cada desafio ────────── */}
        <g aria-hidden className="pointer-events-none">
          {constelacoes.map((c) => {
            const pts = c.pontos
              .map((p) => posicoes.get(p.resolucaoId))
              .filter((p): p is Ponto2D => p != null)
            if (pts.length < 2) return null
            const acesa = destacado?.desafioId === c.desafioId
            return (
              <polyline
                key={c.desafioId}
                points={pts.map((p) => `${p.x},${p.y}`).join(' ')}
                fill="none"
                stroke="var(--steel)"
                strokeWidth={1}
                strokeLinejoin="round"
                opacity={acesa ? 0.5 : escuro ? 0.22 : 0.3}
              />
            )
          })}
        </g>

        {/* ── Decoração da estrela selecionada (fica ABAIXO das estrelas para não roubar
               o alvo de clique nem bagunçar a ordem de tabulação) ──────────── */}
        {selecionado && <DecoracaoSelecionada ponto={selecionado} pos={posicoes.get(selecionado.resolucaoId)!} tema={tema} />}

        {/* ── As estrelas (uma resolução = uma estrela) ──────────────────── */}
        {!carregando && (
          <g>
            {pontos.map((p) => {
              const pos = posicoes.get(p.resolucaoId)
              if (!pos) return null
              return (
                <Estrela
                  key={p.resolucaoId}
                  ponto={p}
                  pos={pos}
                  tema={tema}
                  selecionada={p.resolucaoId === selecionadoId}
                  ativa={p.resolucaoId === destacadoId}
                  focada={p.resolucaoId === focoId}
                  clicavel={clicavel}
                  onSelecionar={onSelecionar}
                  onHover={setHoverId}
                  onFoco={setFocoId}
                />
              )
            })}
          </g>
        )}

        {/* ── Carregando: estrelas-fantasma (sem cor — a ausência de medida não é medida) ── */}
        {carregando && (
          <g aria-hidden className="pointer-events-none">
            {FANTASMAS.map(([a, k], i) => (
              <circle
                key={i}
                cx={xDaAutonomia(a)}
                cy={yDaClasse(k)}
                r={NUCLEO_R}
                fill="var(--line)"
                className="ci-pulse"
                style={{ animationDelay: `${i * 0.12}s` }}
              />
            ))}
          </g>
        )}
      </svg>

      {/* ── Callout do ponto em destaque (HTML sobre o SVG) ──────────────── */}
      {destacado && !carregando && (
        <Callout ponto={destacado} pos={posicoes.get(destacado.resolucaoId)!} tema={tema} />
      )}

      {/* ── Vazio: o céu fica, as estrelas é que não existem ─────────────── */}
      {vazio && <CartaVazia total={dataset.total} semMetrica={dataset.semMetrica.total} />}
    </div>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// ESTRELA
// ════════════════════════════════════════════════════════════════════════════

interface EstrelaProps {
  ponto: PontoPlotavel
  pos: Ponto2D
  tema: PropsGrafico['tema']
  selecionada: boolean
  /** Em destaque (hover, foco ou seleção) — mostra o callout e acende a constelação. */
  ativa: boolean
  focada: boolean
  clicavel: boolean
  onSelecionar?: (resolucaoId: string) => void
  onHover: (id: string | null) => void
  onFoco: (id: string | null) => void
}

function Estrela({
  ponto,
  pos,
  tema,
  selecionada,
  ativa,
  focada,
  clicavel,
  onSelecionar,
  onHover,
  onFoco,
}: EstrelaProps) {
  const escuro = tema === 'dark'
  const cor = corDaClasse(ponto.k, tema)
  /*
   * REGRA 3 (inviolável). MEDIDO × ≈ ESTIMADO é natureza do TIPO da métrica, não do valor:
   * a classe de tempo SEMPRE sai de inferência estática → SEMPRE ≈ ESTIMADO → núcleo VAZADO.
   * (A confiança do motor — ALTA/MEDIA/BAIXA — é outro eixo e vira TEXTO no callout.)
   */
  const medido = CONFIANCA_BIG_O === 'MEDIDO'
  // Núcleo da estrela selecionada: branco-estelar no escuro; no claro, `currentColor` = a
  // cor da classe (o token `--estrela-nucleo` foi desenhado exatamente para isso).
  const nucleoSel = 'var(--estrela-nucleo)'

  const raioHalo = ativa && !selecionada ? HALO_R_HOVER : HALO_R
  const opacidadeHalo = ativa && !selecionada ? 0.35 : 0.2

  const descricao = descrever(ponto)

  return (
    <g
      role={clicavel ? 'button' : 'img'}
      tabIndex={0}
      aria-label={descricao}
      aria-current={selecionada ? 'true' : undefined}
      // `color` alimenta o `currentColor` de `--estrela-nucleo` no modo claro.
      style={{ color: cor }}
      className={cn('outline-none', clicavel && 'cursor-pointer')}
      onClick={() => onSelecionar?.(ponto.resolucaoId)}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          onSelecionar?.(ponto.resolucaoId)
        }
      }}
      onMouseEnter={() => onHover(ponto.resolucaoId)}
      onMouseLeave={() => onHover(null)}
      onFocus={() => onFoco(ponto.resolucaoId)}
      onBlur={() => onFoco(null)}
    >
      <title>{descricao}</title>

      {/* O halo da selecionada já é desenhado (maior) na camada de decoração. */}
      {!selecionada && (
        <circle cx={pos.x} cy={pos.y} r={raioHalo} fill={cor} opacity={escuro ? opacidadeHalo : opacidadeHalo - 0.02} />
      )}

      {/* NÚCLEO — regra 3 (inviolável): MEDIDO é CHEIO, ≈ ESTIMADO é VAZADO. A incerteza da
          métrica é visível na própria estrela, não só no callout. */}
      {medido ? (
        <circle
          cx={pos.x}
          cy={pos.y}
          r={selecionada ? SEL_NUCLEO_R : NUCLEO_R}
          fill={selecionada ? nucleoSel : cor}
        />
      ) : (
        <circle
          cx={pos.x}
          cy={pos.y}
          r={selecionada ? SEL_NUCLEO_R : NUCLEO_R}
          fill="none"
          stroke={selecionada ? nucleoSel : cor}
          strokeWidth={selecionada ? 1.6 : 1.3}
        />
      )}

      {/* Anel de foco de teclado — o SVG não tem box-shadow: o anel é geometria. */}
      {focada && (
        <circle
          cx={pos.x}
          cy={pos.y}
          r={FOCO_R}
          fill="none"
          stroke="var(--ink)"
          strokeWidth={1}
          opacity={0.9}
        />
      )}

      {/* Alvo de clique/toque invisível (fill transparent recebe evento; fill none não). */}
      <circle cx={pos.x} cy={pos.y} r={ALVO_R} fill="transparent" />
    </g>
  )
}

/**
 * O texto que o leitor de tela e o `title` do mouse dizem — a estrela inteira em uma frase.
 * A complexidade é ESTIMADA (nunca "medida"); a confiança do motor entra como qualificador.
 */
function descrever(p: PontoPlotavel): string {
  const lingua = LINGUAGEM_META[p.linguagem]?.label ?? p.linguagem
  const tempo = comPrefixoEstimado(rotuloCanonico(p.k), CONFIANCA_BIG_O).trim()
  const mccabe = p.ciclomatica != null ? ` · ciclomática ${p.ciclomatica} (medida)` : ''
  const natureza = CONFIANCA_BIG_O === 'MEDIDO' ? 'medida' : 'estimada por análise estática'
  const motor = rotuloConfiancaMotor(p.confiancaTempo)
  return `${p.desafioTitulo} · ${lingua} · complexidade de tempo ${tempo} (${natureza}${motor ? `, ${motor}` : ''})${mccabe} · autonomia ${p.autonomia} de 5 · enviada ${dataCompleta(p.submetidaEm)}`
}

// ════════════════════════════════════════════════════════════════════════════
// DECORAÇÃO DA SELEÇÃO — halo, anel, cruzeta e as guias até os dois eixos
// ════════════════════════════════════════════════════════════════════════════

function DecoracaoSelecionada({
  ponto,
  pos,
  tema,
}: {
  ponto: PontoPlotavel
  pos: Ponto2D
  tema: PropsGrafico['tema']
}) {
  const escuro = tema === 'dark'
  const cor = corDaClasse(ponto.k, tema)
  // Token do sistema: branco-estelar no escuro, `currentColor` (= a classe) no claro.
  const cruzeta = 'var(--estrela-nucleo)'

  return (
    <g aria-hidden className="pointer-events-none" style={{ color: cor }}>
      {/* Guias tracejadas até o eixo Y (a classe) e até a base (a autonomia) — na cor da
          classe: é a leitura do par (autonomia × complexidade) daquela resolução. */}
      <line
        x1={pos.x}
        y1={pos.y}
        x2={CARTA_PLOT.x0}
        y2={pos.y}
        stroke={cor}
        strokeWidth={1}
        strokeDasharray="3 3"
        opacity={escuro ? 0.5 : 0.6}
      />
      <line
        x1={pos.x}
        y1={pos.y}
        x2={pos.x}
        y2={CARTA_PLOT.y1}
        stroke={cor}
        strokeWidth={1}
        strokeDasharray="3 3"
        opacity={escuro ? 0.5 : 0.6}
      />

      <circle cx={pos.x} cy={pos.y} r={SEL_HALO_R} fill={cor} opacity={escuro ? 0.22 : 0.2} />
      <circle
        cx={pos.x}
        cy={pos.y}
        r={SEL_ANEL_R}
        fill="none"
        stroke={cor}
        strokeWidth={1}
        opacity={escuro ? 0.5 : 0.6}
      />

      {/* Cruzeta da estrela (os braços do "astro" apontado). */}
      <line
        x1={pos.x}
        y1={pos.y - CRUZETA_BRACO}
        x2={pos.x}
        y2={pos.y + CRUZETA_BRACO}
        stroke={cruzeta}
        strokeWidth={1}
        opacity={escuro ? 0.85 : 0.7}
      />
      <line
        x1={pos.x - CRUZETA_BRACO}
        y1={pos.y}
        x2={pos.x + CRUZETA_BRACO}
        y2={pos.y}
        stroke={cruzeta}
        strokeWidth={1}
        opacity={escuro ? 0.85 : 0.7}
      />
    </g>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// CALLOUT — o rótulo do ponto em destaque
// ════════════════════════════════════════════════════════════════════════════

function Callout({
  ponto,
  pos,
  tema,
}: {
  ponto: PontoPlotavel
  pos: Ponto2D
  tema: PropsGrafico['tema']
}) {
  const escuro = tema === 'dark'
  const cor = corDaClasse(ponto.k, tema)
  const tinta = tintaDaClasse(ponto.k, tema)
  const lingua = LINGUAGEM_META[ponto.linguagem]?.label ?? ponto.linguagem
  const { left, top, transform } = posicaoCallout(pos.x, pos.y)

  const tempo = comPrefixoEstimado(rotuloCanonico(ponto.k), CONFIANCA_BIG_O)
  const motor = rotuloConfiancaMotor(ponto.confiancaTempo)

  return (
    // aria-hidden: tudo isto já está no `aria-label` da estrela — não repetir para o leitor.
    <div
      aria-hidden
      className="pointer-events-none absolute z-10 flex flex-col gap-[3px] whitespace-nowrap rounded-ci bg-elevated px-[10px] py-2 shadow-callout"
      style={{ left, top, transform, border: `1px solid ${cor}` }}
    >
      <span className="font-mono text-[11px] font-semibold text-ink">
        {escuro && <span className="mr-1">✦</span>}
        {ponto.desafioTitulo} · {lingua}
      </span>

      {/*
       * UMA COR POR SEMÂNTICA (regras 1 e 4): só a CLASSE veste o colormap. A ciclomática é
       * contagem (não é classe) e a autonomia é NEUTRA — pintá-las com a tinta da classe diria
       * ao aluno "sua autonomia 4 é laranja porque a solução é O(n²)", que é a associação que
       * este sistema existe para impedir.
       */}
      <span className="tabular flex items-center gap-1.5 font-mono text-[10px]">
        <span style={{ color: tinta }}>{tempo}</span>
        {ponto.ciclomatica != null && <span className="text-mid">· M={ponto.ciclomatica}</span>}
        <span className="text-ink">· aut {ponto.autonomia}/5</span>
      </span>

      {motor && <span className="font-mono text-[9.5px] text-soft">{motor}</span>}
    </div>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// VAZIO
// ════════════════════════════════════════════════════════════════════════════

/**
 * O céu continua lá (grade + colormap + estrelas cenográficas): o vazio da Carta é a ausência
 * de estrelas, não a ausência de gráfico. E o motivo do vazio é dito — se há resoluções mas
 * nenhuma virou ponto, o problema é a métrica (só Java), não a falta de trabalho.
 */
function CartaVazia({ total, semMetrica }: { total: number; semMetrica: number }) {
  const descricao =
    total > 0
      ? `${pluralPt(semMetrica, 'resolução sem métrica', 'resoluções sem métrica')}. ${NOTA_METRICAS_SO_JAVA}`
      : 'Submeta uma resolução em Java para ver sua primeira estrela.'

  return (
    <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center gap-[9px] px-5 text-center">
      <div
        className="flex items-center justify-center rounded-ci border border-line-strong bg-elevated"
        style={{ width: 38, height: 38 }}
      >
        <Folder size={18} strokeWidth={2} className="text-soft" />
      </div>
      <div className="flex flex-col gap-1.5">
        <span className="text-[13px] font-medium text-body">Nenhuma resolução analisada ainda.</span>
        <span className="mx-auto max-w-[46ch] text-[12px] leading-[1.5] text-soft">{descricao}</span>
      </div>
    </div>
  )
}
