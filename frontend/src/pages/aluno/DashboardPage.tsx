import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { LucideIcon } from 'lucide-react'
import { Braces, Code2, Minus, Plus, Target, TrendingDown, TrendingUp } from 'lucide-react'
import type { UseQueryResult } from '@tanstack/react-query'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { Card } from '@/components/ui/card'
import { buttonClasses } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { InfoButton } from '@/components/ui/info-button'
import { AnalysisStatus, ComplexityBadge, LanguageBadge } from '@/components/domain/badges'
import { useAuth } from '@/auth/useAuth'
import { useEvolucao, useResumoDashboard } from '@/features/metricas/hooks'
import {
  COMPLEXIDADE_ORDEM_MAX,
  complexityHexByOrdinal,
  complexityRotuloByOrdinal,
  prettyBigO,
} from '@/domain/enums'
import { cn, formatDate } from '@/lib/utils'
import type {
  AtividadeRecenteDTO,
  DistribuicaoItemDTO,
  EvolucaoMensalDTO,
  GranularidadeTempo,
  ResumoDashboardDTO,
} from '@/types/api'

type ResumoQuery = UseQueryResult<ResumoDashboardDTO>

// ---- helpers de derivação (puros) ----

type Tendencia = 'alta' | 'baixa' | 'estavel' | 'insuficiente'

/** Sinal da variação entre o primeiro e o último valor não-nulo de uma série. */
function tendencia(valores: (number | null)[]): Tendencia {
  const v = valores.filter((x): x is number => x != null)
  if (v.length < 2) return 'insuficiente'
  const delta = v[v.length - 1] - v[0]
  if (delta > 0.05) return 'alta'
  if (delta < -0.05) return 'baixa'
  return 'estavel'
}

/**
 * Mediana ponderada da complexidade a partir da distribuição (por ordinal).
 * Resoluções não classificadas (ordem -1, rótulo "?") ficam de fora: "não sei" não é uma
 * complexidade menor que O(1), e incluí-las puxaria a mediana para baixo. Elas seguem
 * visíveis nas barras de distribuição, onde a contagem é informativa.
 */
function medianaComplexidade(itens: DistribuicaoItemDTO[]): { rotulo: string; ordem: number } | null {
  const classificados = itens.filter((i) => i.ordem >= 0)
  const total = classificados.reduce((s, i) => s + i.total, 0)
  if (total === 0) return null
  const ordenados = [...classificados].sort((a, b) => a.ordem - b.ordem)
  const alvo = Math.ceil(total / 2)
  let acc = 0
  for (const i of ordenados) {
    acc += i.total
    if (acc >= alvo) return { rotulo: i.rotulo, ordem: i.ordem }
  }
  return ordenados[ordenados.length - 1] ?? null
}

function plural(n: number, singular: string, pluralForma: string): string {
  return `${n} ${n === 1 ? singular : pluralForma}`
}

/**
 * Textos dos "?" do dashboard. O de complexidade/evolução explica de propósito
 * que o card (mediana geral) e a linha do gráfico (média por período) são
 * estatísticas diferentes — por isso podem mostrar classes Big O diferentes.
 */
const DASH_INFO = {
  evolucao: {
    titulo: 'Como ler a evolução',
    subtitulo: 'Duas séries ao longo do tempo (mês/semana/dia)',
    secoes: [
      {
        rotulo: 'Linha cheia — Autonomia',
        texto:
          'A média do seu Índice de Autonomia IA (1 a 5) das resoluções enviadas em cada período. Subir significa que você declarou ter feito com mais autonomia.',
      },
      {
        rotulo: 'Linha tracejada — Complexidade',
        texto:
          'A complexidade de tempo (Big O) típica das resoluções de cada período, como uma média aproximada. Serve para ler a tendência: cair costuma indicar soluções mais eficientes com o tempo.',
      },
      {
        rotulo: 'Difere do card ao lado',
        texto:
          'Aqui o valor é por período e aproximado; o card "Complexidade típica" usa a mediana de todo o seu histórico. Por serem estatísticas diferentes, podem mostrar classes diferentes — use o gráfico para a tendência e o card para o valor típico geral.',
      },
    ],
  },
  complexidade: {
    titulo: 'Complexidade típica',
    subtitulo: 'Mediana do seu histórico analisado',
    secoes: [
      {
        rotulo: 'O que é',
        texto:
          'A classe Big O de tempo que fica no meio de todas as suas resoluções já analisadas (mediana). Por ser a mediana, é sempre uma classe real que você de fato escreveu.',
      },
      {
        rotulo: 'Por que a mediana',
        texto:
          'A mediana é robusta a extremos: uma ou outra solução muito custosa (ex.: O(n²)) não distorce o valor típico, ao contrário de uma média.',
      },
      {
        rotulo: 'Difere do gráfico',
        texto:
          'Pode diferir da linha de complexidade do gráfico de Evolução, que é uma média por período (aproximada). Não é erro: são recortes e estatísticas diferentes.',
      },
    ],
  },
  autonomia: {
    titulo: 'Autonomia IA média',
    subtitulo: 'Índice autodeclarado de 1 a 5',
    secoes: [
      {
        rotulo: 'O que é',
        texto:
          'A média do Índice de Autonomia IA que você informa ao enviar cada resolução: 1 = fiz com bastante apoio de IA; 5 = fiz de forma autônoma.',
      },
      {
        rotulo: 'Como ler',
        texto:
          'Quanto maior a média, mais autônomo você declarou ter sido no conjunto das suas soluções. O medidor mostra essa média nos 5 segmentos.',
      },
      {
        rotulo: 'Tendência',
        texto:
          'A legenda "em alta/queda/estável" compara a autonomia das primeiras e das últimas resoluções — um sinal do seu amadurecimento ao longo do tempo.',
      },
    ],
  },
}

// ---- cards ----

/** Card de um número agregado (Desafios / Resoluções / Snippets) + legenda. */
function StatCard({
  icon: Icon,
  label,
  query,
  select,
  caption,
}: {
  icon: LucideIcon
  label: string
  query: ResumoQuery
  select: (r: ResumoDashboardDTO) => number
  caption: (r: ResumoDashboardDTO) => string
}) {
  return (
    <Card className="flex flex-col gap-2.5 p-[18px]">
      <div className="flex items-center justify-between">
        <span className="text-[13px] font-semibold text-muted">{label}</span>
        <div className="flex h-8 w-8 items-center justify-center rounded-[9px] bg-brand/10">
          <Icon size={17} className="text-brand-strong" />
        </div>
      </div>
      {query.isPending ? (
        <Skeleton className="h-[34px] w-16" />
      ) : query.isError ? (
        <span className="font-mono text-[28px] font-bold text-subtle">—</span>
      ) : (
        <>
          <span className="font-mono text-[34px] font-bold leading-none text-heading tabular-nums">
            {select(query.data)}
          </span>
          <span className="text-[12px] text-subtle">{caption(query.data)}</span>
        </>
      )}
    </Card>
  )
}

/** Autonomia IA média: número preciso + medidor de 5 segmentos + tendência. */
function AutonomiaCard({ query }: { query: ResumoQuery }) {
  const media = query.data?.mediaAutonomia
  const trend = tendencia(query.data?.evolucao.map((p) => p.mediaAutonomia) ?? [])
  const trendLabel: Record<Tendencia, string> = {
    alta: 'em alta nos últimos meses',
    baixa: 'em queda nos últimos meses',
    estavel: 'estável nos últimos meses',
    insuficiente: 'sua média atual',
  }
  return (
    <Card className="flex flex-col gap-3 p-[18px]">
      <div className="flex items-center justify-between gap-2">
        <span className="text-[13px] font-semibold text-muted">Autonomia IA média</span>
        <InfoButton
          titulo={DASH_INFO.autonomia.titulo}
          subtitulo={DASH_INFO.autonomia.subtitulo}
          secoes={DASH_INFO.autonomia.secoes}
          ariaLabel="O que é a Autonomia IA média?"
        />
      </div>
      {query.isPending ? (
        <Skeleton className="h-[30px] w-24" />
      ) : query.isError ? (
        <span className="font-mono text-[26px] font-bold text-subtle">—</span>
      ) : media == null ? (
        <span className="text-[13px] text-subtle">Sem resoluções ainda</span>
      ) : (
        <>
          <div className="flex items-baseline gap-1">
            <span className="font-mono text-[32px] font-bold leading-none text-heading tabular-nums">
              {media.toFixed(1).replace('.', ',')}
            </span>
            <span className="font-mono text-[17px] text-subtle">/5</span>
          </div>
          <AutonomyMeter value={media} size="md" showLabel={false} />
          <span className="text-[11.5px] text-subtle">{trendLabel[trend]}</span>
        </>
      )}
    </Card>
  )
}

/** Complexidade típica: mediana das resoluções analisadas. */
function ComplexidadeTipicaCard({ query }: { query: ResumoQuery }) {
  const mediana = query.data ? medianaComplexidade(query.data.distribuicaoBigO) : null
  return (
    <Card className="flex flex-1 flex-col justify-center gap-2 p-[18px]">
      <div className="flex items-center justify-between gap-2">
        <span className="text-[13px] font-semibold text-muted">Complexidade típica</span>
        <InfoButton
          titulo={DASH_INFO.complexidade.titulo}
          subtitulo={DASH_INFO.complexidade.subtitulo}
          secoes={DASH_INFO.complexidade.secoes}
          ariaLabel="O que é a Complexidade típica?"
        />
      </div>
      {query.isPending ? (
        <Skeleton className="h-[28px] w-28" />
      ) : query.isError ? (
        <span className="font-mono text-[24px] font-semibold text-subtle">—</span>
      ) : mediana == null ? (
        <span className="text-[13px] leading-relaxed text-subtle">Sem análises ainda.</span>
      ) : (
        <>
          <span
            className="font-mono text-[30px] font-semibold leading-none"
            style={{ color: complexityHexByOrdinal(mediana.ordem) }}
          >
            {prettyBigO(mediana.rotulo)}
          </span>
          <span className="text-[11.5px] text-subtle">mediana das suas resoluções analisadas</span>
        </>
      )}
    </Card>
  )
}

/** Gráfico de linhas da evolução: autonomia (sólida) × complexidade típica (tracejada). */
function EvolucaoChart({
  pontos,
  granularidade,
}: {
  pontos: EvolucaoMensalDTO[]
  granularidade: GranularidadeTempo
}) {
  const n = pontos.length
  const px = (i: number) => (n <= 1 ? 50 : (i / (n - 1)) * 100)
  const yAut = (v: number) => 100 - (Math.max(0, Math.min(5, v)) / 5) * 100
  const yCx = (v: number) =>
    100 - (Math.max(0, Math.min(COMPLEXIDADE_ORDEM_MAX, v)) / COMPLEXIDADE_ORDEM_MAX) * 100

  const autPts = pontos.map((p, i) =>
    p.mediaAutonomia == null
      ? null
      : {
          x: px(i),
          y: yAut(p.mediaAutonomia),
          label: p.mediaAutonomia.toFixed(1).replace('.', ','),
          title: `Autonomia ${p.mediaAutonomia.toFixed(1).replace('.', ',')} / 5`,
        },
  )
  const cxPts = pontos.map((p, i) =>
    p.mediaComplexidade == null
      ? null
      : {
          x: px(i),
          y: yCx(p.mediaComplexidade),
          label: prettyBigO(complexityRotuloByOrdinal(Math.round(p.mediaComplexidade))),
          title: `Complexidade típica ≈ ${prettyBigO(
            complexityRotuloByOrdinal(Math.round(p.mediaComplexidade)),
          )}`,
        },
  )
  const toPolyline = (pts: ({ x: number; y: number } | null)[]) =>
    pts
      .filter((p): p is { x: number; y: number } => p != null)
      .map((p) => `${p.x},${p.y}`)
      .join(' ')

  // Rótulos de valor quando há poucos pontos (evita o ponto "solto"); a linha só desenha com ≥ 2 pontos.
  const esparso = n <= 3
  const fmtLabel = (p: EvolucaoMensalDTO) =>
    granularidade === 'MENSAL'
      ? `${String(p.mes).padStart(2, '0')}/${String(p.ano).slice(2)}`
      : `${String(p.dia).padStart(2, '0')}/${String(p.mes).padStart(2, '0')}`

  return (
    <div className="flex flex-col gap-2.5">
      <div className="relative h-44 w-full">
        <svg
          viewBox="0 0 100 100"
          preserveAspectRatio="none"
          className="absolute inset-0 h-full w-full"
          aria-hidden
        >
          {[0, 25, 50, 75, 100].map((y) => (
            <line
              key={y}
              x1="0"
              y1={y}
              x2="100"
              y2={y}
              stroke="var(--border)"
              strokeWidth="1"
              vectorEffect="non-scaling-stroke"
            />
          ))}
          {n === 1 &&
            [
              { p: cxPts[0], color: 'var(--warning)' },
              { p: autPts[0], color: 'var(--brand-strong)' },
            ].map(({ p, color }, k) =>
              p == null ? null : (
                <line
                  key={k}
                  x1="0"
                  y1={p.y}
                  x2="100"
                  y2={p.y}
                  stroke={color}
                  strokeWidth="1.5"
                  strokeDasharray="3 3"
                  strokeOpacity="0.5"
                  vectorEffect="non-scaling-stroke"
                />
              ),
            )}
          {n > 1 && (
            <>
              <polyline
                points={toPolyline(cxPts)}
                fill="none"
                stroke="var(--warning)"
                strokeWidth="2"
                strokeDasharray="4 3"
                strokeLinecap="round"
                strokeLinejoin="round"
                vectorEffect="non-scaling-stroke"
              />
              <polyline
                points={toPolyline(autPts)}
                fill="none"
                stroke="var(--brand-strong)"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                vectorEffect="non-scaling-stroke"
              />
            </>
          )}
        </svg>
        {cxPts.map((p, i) =>
          p == null ? null : (
            <span key={`c${i}`}>
              <span
                className="absolute h-2 w-2 -translate-x-1/2 -translate-y-1/2 rounded-full"
                style={{ left: `${p.x}%`, top: `${p.y}%`, background: 'var(--warning)' }}
                title={p.title}
              />
              {esparso && (
                <span
                  className="absolute -translate-x-1/2 translate-y-1.5 whitespace-nowrap font-mono text-[10px] font-semibold"
                  style={{ left: `${p.x}%`, top: `${p.y}%`, color: 'var(--warning)' }}
                >
                  {p.label}
                </span>
              )}
            </span>
          ),
        )}
        {autPts.map((p, i) =>
          p == null ? null : (
            <span key={`a${i}`}>
              <span
                className="absolute h-2.5 w-2.5 -translate-x-1/2 -translate-y-1/2 rounded-full ring-2 ring-surface"
                style={{ left: `${p.x}%`, top: `${p.y}%`, background: 'var(--brand-strong)' }}
                title={p.title}
              />
              {esparso && (
                <span
                  className="absolute -translate-x-1/2 -translate-y-[15px] whitespace-nowrap font-mono text-[10px] font-semibold text-brand-strong"
                  style={{ left: `${p.x}%`, top: `${p.y}%` }}
                >
                  {p.label}
                </span>
              )}
            </span>
          ),
        )}
      </div>
      <div className="flex w-full">
        {pontos.map((p) => (
          <span
            key={`${p.ano}-${p.mes}-${p.dia}`}
            className="flex-1 text-center font-mono text-[10px] text-subtle"
          >
            {fmtLabel(p)}
          </span>
        ))}
      </div>
    </div>
  )
}

function LegendSwatch({ color, dashed, label }: { color: string; dashed?: boolean; label: string }) {
  return (
    <span className="flex items-center gap-1.5 text-[12px] text-muted">
      <span
        className="inline-block w-3.5"
        style={
          dashed
            ? { borderTop: `2px dashed ${color}`, height: 0 }
            : { height: 3, borderRadius: 2, background: color }
        }
      />
      {label}
    </span>
  )
}

const GRANULARIDADES: { valor: GranularidadeTempo; label: string }[] = [
  { valor: 'MENSAL', label: 'Mês' },
  { valor: 'SEMANAL', label: 'Semana' },
  { valor: 'DIARIO', label: 'Dia' },
]

/** Evolução: gráfico de linhas + legenda + seletor de granularidade (mês/semana/dia). */
function EvolucaoCard() {
  const [gran, setGran] = useState<GranularidadeTempo>('MENSAL')
  const query = useEvolucao(gran)
  const pontos = query.data ?? []
  const escala = gran === 'MENSAL' ? 'mês' : gran === 'SEMANAL' ? 'semana' : 'dia'
  return (
    <Card className="flex flex-col gap-4 p-[18px]">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex flex-col gap-0.5">
          <div className="flex items-center gap-1.5">
            <span className="text-[14px] font-bold text-heading">Evolução</span>
            <InfoButton
              titulo={DASH_INFO.evolucao.titulo}
              subtitulo={DASH_INFO.evolucao.subtitulo}
              secoes={DASH_INFO.evolucao.secoes}
              ariaLabel="Como ler o gráfico de evolução?"
            />
          </div>
          <span className="text-[12px] text-subtle">
            Autonomia IA × complexidade típica por {escala}
          </span>
        </div>
        <div className="flex flex-col items-end gap-2">
          <div
            role="group"
            aria-label="Granularidade do gráfico"
            className="inline-flex rounded-lg border border-border bg-surface-2 p-0.5"
          >
            {GRANULARIDADES.map((g) => (
              <button
                key={g.valor}
                type="button"
                onClick={() => setGran(g.valor)}
                aria-pressed={gran === g.valor}
                className={cn(
                  'rounded-md px-2.5 py-1 text-[12px] font-semibold transition-colors',
                  gran === g.valor ? 'bg-brand text-white' : 'text-muted hover:text-fg',
                )}
              >
                {g.label}
              </button>
            ))}
          </div>
          <div className="flex gap-3.5">
            <LegendSwatch color="var(--brand-strong)" label="Autonomia" />
            <LegendSwatch color="var(--warning)" dashed label="Complexidade" />
          </div>
        </div>
      </div>
      {query.isPending ? (
        <Skeleton className="h-44 w-full rounded" />
      ) : query.isError ? (
        <span className="text-[13px] text-subtle">Não foi possível carregar.</span>
      ) : pontos.length === 0 ? (
        <div className="flex h-44 items-center justify-center">
          <span className="max-w-xs text-center text-[13px] leading-relaxed text-subtle">
            Sem resoluções ainda — o gráfico de evolução aparece conforme você submete soluções.
          </span>
        </div>
      ) : (
        <EvolucaoChart pontos={pontos} granularidade={gran} />
      )}
    </Card>
  )
}

/** Uma linha da lista "Atividade recente". */
function AtividadeRow({ a }: { a: AtividadeRecenteDTO }) {
  return (
    <Link
      to={`/app/resolucoes/${a.resolucaoId}`}
      className="flex items-center gap-3.5 border-t border-border-subtle px-3.5 py-[11px] transition-colors first:border-t-0 hover:bg-surface-2"
    >
      <LanguageBadge linguagem={a.linguagem} className="shrink-0" />
      <span className="min-w-0 flex-1 truncate text-[13.5px] text-fg">{a.desafioTitulo}</span>
      <AutonomyMeter
        value={a.indiceAutonomiaIA}
        size="sm"
        showLabel={false}
        className="hidden sm:inline-flex"
      />
      <span className="shrink-0">
        {!a.analisada ? (
          <AnalysisStatus analisada={false} />
        ) : a.complexidadeRotulo ? (
          <ComplexityBadge rotulo={a.complexidadeRotulo} valor={a.complexidadeOrdem ?? undefined} />
        ) : (
          <span className="font-mono text-[11px] text-neutral">sem métrica</span>
        )}
      </span>
      <span className="hidden w-[76px] shrink-0 text-right font-mono text-[11.5px] text-subtle tabular-nums sm:inline">
        {formatDate(a.submetidaEm)}
      </span>
    </Link>
  )
}

/** Atividade recente: últimas resoluções submetidas. */
function AtividadeRecenteCard({ query }: { query: ResumoQuery }) {
  const itens = query.data?.atividadeRecente ?? []
  return (
    <Card className="flex flex-col overflow-hidden p-0">
      <div className="flex items-center justify-between px-3.5 pb-2.5 pt-3">
        <span className="text-[14px] font-bold text-heading">Atividade recente</span>
        <Link to="/app/desafios" className="text-[12px] font-medium text-brand-strong hover:underline">
          Ver tudo
        </Link>
      </div>
      {query.isPending ? (
        <div className="flex flex-col gap-2 p-3.5 pt-1">
          {[0, 1, 2, 3].map((k) => (
            <Skeleton key={k} className="h-9 w-full rounded" />
          ))}
        </div>
      ) : query.isError ? (
        <span className="px-3.5 py-6 text-[13px] text-subtle">Não foi possível carregar.</span>
      ) : itens.length === 0 ? (
        <span className="px-3.5 py-8 text-center text-[13px] leading-relaxed text-subtle">
          Nenhuma resolução ainda — submeta uma solução para ver sua atividade aqui.
        </span>
      ) : (
        <div className="flex flex-col">
          {itens.map((a) => (
            <AtividadeRow key={a.resolucaoId} a={a} />
          ))}
        </div>
      )}
    </Card>
  )
}

/** Distribuição de complexidade (Big O de tempo) em barras horizontais + rodapé de leitura. */
function DistribuicaoCard({ query }: { query: ResumoQuery }) {
  const itens = query.data ? [...query.data.distribuicaoBigO].sort((a, b) => a.ordem - b.ordem) : []
  const max = Math.max(1, ...itens.map((i) => i.total))
  const cxTrend = tendencia(query.data?.evolucao.map((p) => p.mediaComplexidade) ?? [])
  const rodape: Record<Tendencia, { Icon: LucideIcon; color: string; texto: string }> = {
    baixa: {
      Icon: TrendingDown,
      color: 'var(--success)',
      texto: 'Suas soluções estão ficando mais eficientes.',
    },
    alta: {
      Icon: TrendingUp,
      color: 'var(--warning)',
      texto: 'A complexidade típica das suas soluções vem subindo.',
    },
    estavel: {
      Icon: Minus,
      color: 'var(--subtle)',
      texto: 'Complexidade típica estável entre os meses.',
    },
    insuficiente: {
      Icon: Minus,
      color: 'var(--subtle)',
      texto: 'Distribuição das suas resoluções analisadas.',
    },
  }
  const foot = rodape[cxTrend]

  return (
    <Card className="flex flex-col gap-3.5 p-[18px]">
      <span className="text-[14px] font-bold text-heading">Distribuição de complexidade</span>
      {query.isPending ? (
        <div className="flex flex-col gap-2.5">
          {[0, 1, 2, 3].map((k) => (
            <Skeleton key={k} className="h-5 w-full rounded" />
          ))}
        </div>
      ) : query.isError ? (
        <span className="text-[13px] text-subtle">Não foi possível carregar.</span>
      ) : itens.length === 0 ? (
        <span className="text-[13px] leading-relaxed text-subtle">
          Sem análises ainda — submeta resoluções em Java para ver a distribuição.
        </span>
      ) : (
        <>
          <div className="flex flex-col gap-2.5">
            {itens.map((i) => {
              const cor = complexityHexByOrdinal(i.ordem)
              return (
                <div key={i.rotulo} className="flex items-center gap-2.5">
                  <span
                    className="w-14 shrink-0 text-right font-mono text-[11.5px] font-semibold"
                    style={{ color: cor }}
                  >
                    {prettyBigO(i.rotulo)}
                  </span>
                  <div className="h-2 flex-1 overflow-hidden rounded-full bg-surface-2">
                    <div
                      className="h-full rounded-full"
                      style={{ width: `${(i.total / max) * 100}%`, minWidth: 6, background: cor }}
                    />
                  </div>
                  <span className="w-4 shrink-0 text-right font-mono text-[11.5px] tabular-nums text-subtle">
                    {i.total}
                  </span>
                </div>
              )
            })}
          </div>
          <div className="flex items-center gap-2 border-t border-border-subtle pt-3">
            <foot.Icon size={15} style={{ color: foot.color }} />
            <span className="text-[12px] text-muted">{foot.texto}</span>
          </div>
        </>
      )}
    </Card>
  )
}

export function DashboardPage() {
  const { user } = useAuth()
  const resumoQuery = useResumoDashboard()

  const captionDesafios = (r: ResumoDashboardDTO) =>
    `${plural(r.desafiosPublicos, 'público', 'públicos')} · ${plural(
      r.totalDesafios - r.desafiosPublicos,
      'privado',
      'privados',
    )}`
  const captionResolucoes = (r: ResumoDashboardDTO) =>
    `${plural(r.resolucoesAnalisadas, 'analisada', 'analisadas')} · ${
      r.totalResolucoes - r.resolucoesAnalisadas
    } calculando`
  const captionSnippets = (r: ResumoDashboardDTO) =>
    `em ${plural(r.totalCategorias, 'categoria', 'categorias')}`

  return (
    <PageContainer>
      <PageHeader
        title={`Olá, ${user?.username ?? 'aluno'}`}
        subtitle="Aqui está o resumo do seu portfólio."
        actions={
          <>
            <Link to="/app/snippets" className={buttonClasses({ variant: 'secondary' })}>
              <Braces size={15} />
              Novo snippet
            </Link>
            <Link to="/app/desafios" className={buttonClasses({ variant: 'primary' })}>
              <Plus size={16} />
              Novo desafio
            </Link>
          </>
        }
      />

      {/* Totais (agregados reais do backend). */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard
          icon={Target}
          label="Desafios"
          query={resumoQuery}
          select={(r) => r.totalDesafios}
          caption={captionDesafios}
        />
        <StatCard
          icon={Code2}
          label="Resoluções"
          query={resumoQuery}
          select={(r) => r.totalResolucoes}
          caption={captionResolucoes}
        />
        <StatCard
          icon={Braces}
          label="Snippets"
          query={resumoQuery}
          select={(r) => r.totalSnippets}
          caption={captionSnippets}
        />
      </div>

      {/* Evolução (variável central da pesquisa) + resumo de autonomia/complexidade. */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1.62fr_1fr]">
        <EvolucaoCard />
        <div className="flex flex-col gap-4">
          <AutonomiaCard query={resumoQuery} />
          <ComplexidadeTipicaCard query={resumoQuery} />
        </div>
      </div>

      {/* Atividade recente (resoluções) + distribuição de complexidade. */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1.62fr_1fr]">
        <AtividadeRecenteCard query={resumoQuery} />
        <DistribuicaoCard query={resumoQuery} />
      </div>
    </PageContainer>
  )
}
