import { Link } from 'react-router-dom'
import { ArrowRight, Braces, Compass, Cpu, Gauge, Target, type LucideIcon } from 'lucide-react'
import { buttonClasses } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { ConfidenceChip, StatusChip } from '@/components/domain/badges'
import { Nebula } from '@/components/Nebula'
import { Starfield } from '@/components/Starfield'
import {
  type ClasseK,
  type Confianca,
  NOTA_METRICAS_SO_JAVA,
  comPrefixoEstimado,
  corDaClasse,
  rotuloCanonico,
  tintaDaClasse,
} from '@/domain/enums'
import { useTheme } from '@/theme/ThemeProvider'

/* ============================================================================
 * B · Landing (spec 03 §B) — a vitrine pública. Tela ESTÁTICA: não consome API,
 * não tem hook de dados, não tem estado de loading/erro.
 *
 * ⚠ TUDO o que a preview da carta mostra é DADO ILUSTRATIVO (não há usuário
 * logado numa landing). São constantes de módulo, marcadas como tal, e passam
 * pelas MESMAS regras de honestidade do produto: o que é estimado leva `≈` e
 * marcador vazado; o que é medido leva marcador cheio. Nada aqui finge ser
 * medição real, e nada aqui esconde a incerteza.
 * ========================================================================== */

// ---------------------------------------------------------------- dados ilustrativos

interface PontoIlustrativo {
  id: string
  /** Agrupa a constelação: resoluções do MESMO desafio ligam-se em ordem cronológica. */
  desafio: string
  legenda: string
  /** Eixo X — Índice de Autonomia IA autodeclarado (1–5; maior = mais autônomo). */
  autonomia: 1 | 2 | 3 | 4 | 5
  /** Eixo Y — classe de complexidade de TEMPO (k = ordem do colormap, 0..7). */
  k: ClasseK
}

/** Exemplo fictício de portfólio — a trajetória que a pesquisa mede (força bruta + IA → refinado + autônomo). */
const PONTOS_ILUSTRATIVOS: PontoIlustrativo[] = [
  { id: 'fib-1', desafio: 'Fibonacci', legenda: 'recursão ingênua', autonomia: 1, k: 6 },
  { id: 'fib-2', desafio: 'Fibonacci', legenda: 'memoização', autonomia: 3, k: 2 },
  { id: 'two-sum-1', desafio: 'Two Sum', legenda: 'força bruta', autonomia: 4, k: 4 },
  { id: 'two-sum-2', desafio: 'Two Sum', legenda: 'HashMap', autonomia: 5, k: 2 },
  { id: 'busca-1', desafio: 'Busca binária', legenda: 'iterativa', autonomia: 2, k: 1 },
  { id: 'merge-1', desafio: 'Merge Sort', legenda: 'divisão e conquista', autonomia: 5, k: 3 },
]

/** A estrela em foco na preview — as três leituras abaixo do gráfico são as dela. */
const SELECIONADO_ID = 'two-sum-1'

interface LeituraIlustrativa {
  rotulo: string
  valor: string
  confianca: Confianca
  /** Só as classes Big-O têm `k`; a ciclomática é contagem, não classe. */
  k: ClasseK | null
}

const LEITURAS_ILUSTRATIVAS: LeituraIlustrativa[] = [
  { rotulo: 'Tempo', valor: rotuloCanonico(4), confianca: 'ESTIMADO', k: 4 },
  { rotulo: 'Ciclo.', valor: 'M = 4', confianca: 'MEDIDO', k: null },
  { rotulo: 'Espaço', valor: rotuloCanonico(0), confianca: 'ESTIMADO', k: 0 },
]

/** Autonomia autodeclarada da resolução em foco (neutra — nunca colormap). */
const AUTONOMIA_ILUSTRATIVA = 4

// ---------------------------------------------------------------- geometria da mini-carta

const CARTA = {
  largura: 380,
  altura: 210,
  topo: 18,
  base: 186,
  esquerda: 60,
  direita: 340,
  eixoY: 34,
  barraX: 24,
  barraW: 6,
} as const

const TOTAL_CLASSES_CARTA = 8
const FAIXA = (CARTA.base - CARTA.topo) / TOTAL_CLASSES_CARTA

/** Autonomia (1..5) → x. */
const xDe = (autonomia: number) =>
  CARTA.esquerda + ((autonomia - 1) / 4) * (CARTA.direita - CARTA.esquerda)

/** Classe de tempo (k) → y (centro da faixa; k=0 embaixo, k=7 no topo). */
const yDe = (k: number) => CARTA.base - (k + 0.5) * FAIXA

/** Constelações = resoluções do mesmo desafio, em ordem cronológica (§6-A, Lacuna 11). */
const CONSTELACOES: PontoIlustrativo[][] = Object.values(
  PONTOS_ILUSTRATIVOS.reduce<Record<string, PontoIlustrativo[]>>((mapa, ponto) => {
    ;(mapa[ponto.desafio] ??= []).push(ponto)
    return mapa
  }, {}),
).filter((grupo) => grupo.length >= 2)

/** Estrelas de cenário do fundo do gráfico (decorativas — não são dado). */
const ESTRELAS_CENARIO = [
  { cx: 90, cy: 40, r: 0.9, o: 0.7 },
  { cx: 220, cy: 30, r: 0.7, o: 0.45 },
  { cx: 320, cy: 52, r: 0.8, o: 0.55 },
  { cx: 150, cy: 150, r: 0.7, o: 0.45 },
]

// ---------------------------------------------------------------- conteúdo estático

/*
 * ⚠ PONTOS NEUTROS, e não do colormap. O colormap é a única cor do sistema PORQUE `k`
 * SIGNIFICA alguma coisa: verde = eficiente, âmbar = O(n log n). Usar `cx-1`/`cx-3` como
 * bolinha de marca de plataforma — na MESMA página em que o preview da carta ensina o
 * visitante a ler essas cores — corrói a codificação: o leitor treinado passa a ler
 * "Neps Academy = O(n log n)". Cor sem significado é ruído (regra 1 · §2.4).
 */
const PLATAFORMAS: { nome: string; ponto: string }[] = [
  { nome: 'LeetCode', ponto: 'bg-steel' },
  { nome: 'Codeforces', ponto: 'bg-steel' },
  { nome: 'Neps Academy', ponto: 'bg-steel' },
]

const FEATURES: { icon: LucideIcon; titulo: string; texto: string; nota?: string }[] = [
  {
    icon: Target,
    titulo: 'Desafios & Resoluções',
    texto: 'Organize exercícios e todas as suas tentativas de solução.',
  },
  {
    icon: Cpu,
    titulo: 'Métricas estáticas',
    texto: 'Big O de tempo e espaço, e ciclomática de McCabe por resolução.',
    nota: NOTA_METRICAS_SO_JAVA,
  },
  {
    icon: Gauge,
    titulo: 'Autonomia IA',
    texto: 'Um índice autodeclarado de 1 a 5 para acompanhar seu amadurecimento.',
  },
  {
    icon: Braces,
    titulo: 'Snippets categorizados',
    texto: 'Guarde trechos por conceito: recursão, grafos, DP e mais.',
  },
]

// ---------------------------------------------------------------- subcomponentes locais

/**
 * Tile compacto de leitura da preview (spec 03 §B.2.3) — irmão menor do
 * `MetricTile` da tela D: mesmo contrato de honestidade (valor com `≈` quando
 * estimado, cor da classe no valor estimado, `ConfidenceChip` com marcador
 * cheio/vazado), em 19px em vez de 31px.
 */
function TileLeitura({ rotulo, valor, confianca, k }: LeituraIlustrativa) {
  const { theme } = useTheme()
  const cor = confianca === 'ESTIMADO' && k !== null ? tintaDaClasse(k, theme) : 'var(--ink)'

  return (
    <div className="flex flex-1 flex-col gap-[5px] rounded-ci border border-line bg-recess px-[11px] py-2.5">
      <span className="font-mono text-[9.5px] uppercase tracking-[.06em] text-mid">{rotulo}</span>
      <span
        className="font-mono text-[19px] leading-none font-semibold tabular-nums"
        style={{ color: cor }}
      >
        {comPrefixoEstimado(valor, confianca)}
      </span>
      <ConfidenceChip tipo={confianca} compact className="self-start" />
    </div>
  )
}

/**
 * Mini-carta celeste — o gráfico-assinatura em miniatura, SVG inline, sem lib.
 * Eixo X = autonomia (1→5) · Eixo Y = classe de complexidade de tempo (colormap).
 */
function MiniCarta() {
  const { theme } = useTheme()
  const selecionado = PONTOS_ILUSTRATIVOS.find((p) => p.id === SELECIONADO_ID)

  return (
    <div className="rounded-ci border border-line bg-panel-chart px-3 pt-3 pb-1.5">
      <svg
        viewBox={`0 0 ${CARTA.largura} ${CARTA.altura}`}
        width="100%"
        role="img"
        aria-labelledby="carta-exemplo-titulo carta-exemplo-desc"
        className="block h-auto w-full"
      >
        <title id="carta-exemplo-titulo">Exemplo de carta celeste</title>
        <desc id="carta-exemplo-desc">
          Gráfico ilustrativo com {PONTOS_ILUSTRATIVOS.length} resoluções fictícias. O eixo
          horizontal é o Índice de Autonomia IA (1 a 5) e o eixo vertical é a classe de
          complexidade de tempo, do verde O(1) ao vermelho O(n!). Resoluções do mesmo desafio são
          ligadas por uma linha, formando a trajetória do aluno.
        </desc>

        {/* grade e eixos */}
        {[18, 66, 114, 162].map((y) => (
          <line
            key={y}
            x1={CARTA.eixoY}
            y1={y}
            x2={CARTA.largura - 20}
            y2={y}
            className="stroke-graf-grade"
            strokeWidth={1}
          />
        ))}
        <line
          x1={CARTA.eixoY}
          y1={CARTA.base}
          x2={CARTA.largura - 20}
          y2={CARTA.base}
          className="stroke-graf-eixo"
          strokeWidth={1}
        />
        <line
          x1={CARTA.eixoY}
          y1={CARTA.topo}
          x2={CARTA.eixoY}
          y2={CARTA.base}
          className="stroke-graf-eixo"
          strokeWidth={1}
        />

        {/* estrelas de cenário (decorativas) */}
        {ESTRELAS_CENARIO.map((e) => (
          <circle
            key={`${e.cx}-${e.cy}`}
            cx={e.cx}
            cy={e.cy}
            r={e.r}
            className="fill-ink"
            opacity={e.o}
          />
        ))}

        {/* eixo Y = as 8 classes do colormap (verde embaixo → vermelho no topo) */}
        {Array.from({ length: TOTAL_CLASSES_CARTA }, (_, k) => (
          <rect
            key={k}
            x={CARTA.barraX}
            y={CARTA.topo + (TOTAL_CLASSES_CARTA - 1 - k) * FAIXA}
            width={CARTA.barraW}
            height={FAIXA}
            fill={corDaClasse(k, theme)}
          />
        ))}

        {/* constelações: mesma cor de linha do sistema (steel), sempre atrás das estrelas */}
        {CONSTELACOES.map((grupo) => (
          <polyline
            key={grupo[0].desafio}
            points={grupo.map((p) => `${xDe(p.autonomia)},${yDe(p.k)}`).join(' ')}
            fill="none"
            className="stroke-steel"
            strokeWidth={1}
            strokeOpacity={0.35}
          />
        ))}

        {/* estrelas-resolução: halo tonal + núcleo, na cor da classe de tempo */}
        {PONTOS_ILUSTRATIVOS.map((p) => {
          const cor = corDaClasse(p.k, theme)
          return (
            <g key={p.id}>
              <circle cx={xDe(p.autonomia)} cy={yDe(p.k)} r={6} fill={cor} opacity={0.2} />
              <circle cx={xDe(p.autonomia)} cy={yDe(p.k)} r={2.3} fill={cor} />
            </g>
          )
        })}

        {/* estrela em foco: halo maior + núcleo claro + cruzeta */}
        {selecionado && (
          <g style={{ color: corDaClasse(selecionado.k, theme) }}>
            <circle
              cx={xDe(selecionado.autonomia)}
              cy={yDe(selecionado.k)}
              r={10}
              fill="currentColor"
              opacity={0.22}
            />
            <circle
              cx={xDe(selecionado.autonomia)}
              cy={yDe(selecionado.k)}
              r={3}
              fill="var(--estrela-nucleo)"
            />
            <line
              x1={xDe(selecionado.autonomia) - 12}
              y1={yDe(selecionado.k)}
              x2={xDe(selecionado.autonomia) + 12}
              y2={yDe(selecionado.k)}
              stroke="var(--estrela-nucleo)"
              strokeWidth={1}
              opacity={0.8}
            />
            <line
              x1={xDe(selecionado.autonomia)}
              y1={yDe(selecionado.k) - 12}
              x2={xDe(selecionado.autonomia)}
              y2={yDe(selecionado.k) + 12}
              stroke="var(--estrela-nucleo)"
              strokeWidth={1}
              opacity={0.8}
            />
          </g>
        )}

        <text
          x={200}
          y={204}
          textAnchor="middle"
          fontSize={9}
          letterSpacing={1}
          className="fill-soft font-mono"
        >
          AUTONOMIA IA →
        </text>
      </svg>
    </div>
  )
}

/** Cartão de preview da carta (coluna direita do hero). */
function PreviewDaCarta() {
  const selecionado = PONTOS_ILUSTRATIVOS.find((p) => p.id === SELECIONADO_ID)

  return (
    <div className="relative flex flex-col gap-3 rounded-ci border border-line bg-panel p-4">
      <div className="flex items-center justify-between gap-3 px-0.5">
        <span className="font-mono text-[10.5px] tracking-[.1em] text-soft uppercase">
          Carta — ana.dev
        </span>
        <StatusChip status="publico" />
      </div>

      <MiniCarta />

      <div className="flex items-center justify-between gap-3 px-0.5">
        <span className="font-mono text-[10.5px] text-soft">
          {selecionado ? `${selecionado.desafio} · ${selecionado.legenda}` : ''}
        </span>
        <span className="font-mono text-[10.5px] text-soft">exemplo ilustrativo</span>
      </div>

      <div className="flex gap-[9px]">
        {LEITURAS_ILUSTRATIVAS.map((leitura) => (
          <TileLeitura key={leitura.rotulo} {...leitura} />
        ))}
      </div>

      <div className="flex items-center justify-between gap-3 rounded-ci border border-line bg-recess px-3 py-2.5">
        <span className="font-mono text-[11px] tracking-[.06em] text-mid uppercase">
          Autonomia IA
        </span>
        <AutonomyMeter value={AUTONOMIA_ILUSTRATIVA} size="md" />
      </div>
    </div>
  )
}

// ---------------------------------------------------------------- tela

export function LandingPage() {
  return (
    <div className="flex flex-col">
      {/* ---------------------------------------------------------------- hero */}
      <section className="relative overflow-hidden">
        <Nebula variant="landing" />
        <Starfield density="esparsa" />

        <div className="relative mx-auto grid w-full max-w-[1320px] grid-cols-1 items-center gap-11 px-6 pt-14 pb-12 sm:px-11 lg:grid-cols-[1.02fr_1fr]">
          {/* coluna esquerda */}
          <div className="flex flex-col items-start gap-[22px]">
            <Badge>
              <span aria-hidden className="ci-blink size-[6px] shrink-0 rounded-full bg-ink" />
              Iniciação científica · portfólio + métricas
            </Badge>

            <h1 className="text-[34px] leading-[1.03] font-bold tracking-[-.035em] text-balance text-ink sm:text-[40px] lg:text-[47px]">
              Uma carta celeste
              <br />
              do seu código.
            </h1>

            <p className="max-w-[466px] text-[16px] leading-[1.62] text-mid">
              Cada resolução é uma estrela, posicionada por{' '}
              <span className="font-medium text-ink">autonomia</span> e{' '}
              <span className="font-medium text-ink">complexidade</span>. Meça a evolução do seu
              raciocínio — com honestidade sobre o que é{' '}
              <span className="font-medium text-ink">medido</span> e o que é{' '}
              <span className="text-mid">estimado</span>.
            </p>

            <div className="mt-0.5 flex flex-wrap gap-[11px]">
              <Link
                to="/criar-conta"
                className={buttonClasses({ font: 'sans', size: 'lg', className: 'gap-[9px]' })}
              >
                Criar conta
                <ArrowRight size={16} strokeWidth={2} aria-hidden />
              </Link>
              {/*
               * §6-A (Lacuna 1): "Ver um portfólio" leva à Explorar PÚBLICA (/explorar),
               * não à autenticada — um visitante nunca deve cair no login por um CTA.
               */}
              <Link
                to="/explorar"
                className={buttonClasses({
                  variant: 'secondary',
                  font: 'sans',
                  size: 'lg',
                  className: 'gap-[9px]',
                })}
              >
                Ver um portfólio
                <Compass size={15} strokeWidth={2} aria-hidden className="text-mid" />
              </Link>
            </div>

            <div className="mt-1.5 flex flex-wrap items-center gap-[18px] font-mono text-[11.5px]">
              <span className="tracking-[.1em] text-soft uppercase">Fontes</span>
              {PLATAFORMAS.map((p) => (
                <span key={p.nome} className="inline-flex items-center gap-1.5 text-mid">
                  <span aria-hidden className={`size-[6px] shrink-0 rounded-full ${p.ponto}`} />
                  {p.nome}
                </span>
              ))}
            </div>
          </div>

          {/* coluna direita — preview da carta (dados ilustrativos) */}
          <PreviewDaCarta />
        </div>
      </section>

      {/* ------------------------------------------------------------ features */}
      <section className="mx-auto w-full max-w-[1320px] px-6 pb-[42px] sm:px-11">
        <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map(({ icon: Icon, titulo, texto, nota }) => (
            <div
              key={titulo}
              className="flex flex-col gap-[11px] rounded-ci border border-line bg-panel p-[18px]"
            >
              <span className="flex size-[34px] shrink-0 items-center justify-center rounded-ci border border-line-strong bg-recess">
                <Icon size={17} strokeWidth={2} aria-hidden className="text-ink" />
              </span>
              <span className="text-[13.5px] font-semibold text-ink">{titulo}</span>
              <span className="text-[12px] leading-[1.5] text-mid">{texto}</span>
              {nota && <span className="font-mono text-[10.5px] leading-[1.45] text-soft">{nota}</span>}
            </div>
          ))}
        </div>
      </section>

      {/* -------------------------------------------------------------- rodapé */}
      <footer className="border-t border-line-soft">
        <div className="mx-auto flex w-full max-w-[1320px] flex-wrap items-center justify-between gap-5 px-6 py-5 sm:px-11">
          {/* Prosa lê em Space Grotesk; mono é para MEDIR (regra 5). Esta frase não mede nada. */}
          <span className="max-w-[560px] text-[12.5px] leading-[1.5] text-soft">
            Projeto de Iniciação Científica — pesquisa sobre autonomia e amadurecimento algorítmico.
          </span>
          <nav aria-label="Links do rodapé" className="flex gap-[18px] font-mono text-[11.5px]">
            <Link to="/explorar" className="text-mid hover:text-ink">
              explorar
            </Link>
            <Link to="/entrar" className="text-mid hover:text-ink">
              entrar
            </Link>
            <Link to="/criar-conta" className="text-mid hover:text-ink">
              criar conta
            </Link>
          </nav>
        </div>
      </footer>
    </div>
  )
}
