/*
 * C · DASHBOARD (spec 04 §2) — a carta do portfólio do aluno.
 *
 * Composição: saudação + ações · 3 stats · painel de gráficos (+ estrela selecionada) ·
 * complexidade típica · distribuição · atividade recente.
 *
 * O gráfico NÃO mora mais aqui: o SVG artesanal de "Evolução" foi substituído pelo
 * `PainelDeGraficos` (Carta · Linha · Matriz), que traz o seletor, os estados de cada
 * visualização e o rodapé honesto ("N de M resoluções plotadas · X sem métrica"). A
 * página só busca o dado (`useCartaCeleste` + `montarDataset`) e é dona da seleção — que
 * também alimenta o `PainelEstrelaSelecionada` ao lado.
 *
 * ⚠ Das 5 visualizações originais restaram 3:
 * · "Espectro" saiu do seletor — era a mesma distribuição duas vezes na mesma tela, e a única
 *   que não plotava resoluções (logo, não tinha o que selecionar). O histograma por classe
 *   continua aqui, no `DistribuicaoCard` (`linhasEspectro`), e agora é o ÚNICO.
 * · "Espiral" (ex-Órbitas) saiu — era redundante com a Linha temporal: mesma pergunta
 *   (autonomia × tempo, complexidade × tempo) por canais perceptuais mais fracos (tamanho e
 *   ângulo, em vez de posição num eixo). Menos gráficos, cada um dizendo uma coisa.
 */
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import type { LucideIcon } from 'lucide-react'
import { Braces, Code2, Gauge, Plus, RefreshCw, Target } from 'lucide-react'
import type { UseQueryResult } from '@tanstack/react-query'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { ErrorState } from '@/components/page/states'
import { Card } from '@/components/ui/card'
import { buttonClasses } from '@/components/ui/button'
import { EmptyState } from '@/components/ui/empty-state'
import { InfoButton } from '@/components/ui/info-button'
import { Skeleton } from '@/components/ui/skeleton'
import { BarraColormap } from '@/components/BarraColormap'
import { AtividadeLinha } from '@/components/domain/AtividadeLinha'
import { ConfidenceChip } from '@/components/domain/badges'
import {
  type DatasetCarta,
  GRANULARIDADE_PADRAO,
  type Granularidade,
  PainelDeGraficos,
  PainelEstrelaSelecionada,
  grupoDeIrmaos,
  linhasEspectro,
  montarDataset,
  pontoPorId,
  useGraficoNaUrl,
} from '@/components/charts'
import { useAuth } from '@/auth/useAuth'
import { useCartaCeleste, useResumoDashboard } from '@/features/metricas/hooks'
import {
  type ClasseK,
  CONFIANCA_BIG_O,
  NOTA_METRICAS_SO_JAVA,
  ROTULO_DESCONHECIDO,
  comPrefixoEstimado,
  corDaClasse,
  rotuloCanonico,
  tintaDaClasse,
} from '@/domain/enums'
import { useTheme } from '@/theme/ThemeProvider'
import { apiErrorMessage } from '@/lib/api'
import { cn, pluralPt } from '@/lib/utils'
import type { ResumoDashboardDTO } from '@/types/api'

type ResumoQuery = UseQueryResult<ResumoDashboardDTO>

// ════════════════════════════════════════════════════════════════════════════
// HELPERS PUROS (preservados da versão anterior)
// ════════════════════════════════════════════════════════════════════════════

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
 * Mediana da classe de complexidade das resoluções PLOTÁVEIS.
 *
 * ⚠ UMA FONTE SÓ. Antes, este card lia `resumo.distribuicaoBigO` (endpoint /resumo) enquanto o
 * Espectro ao lado lia a carta celeste (/carta): duas estatísticas do MESMO dado, com populações
 * diferentes, exibidas lado a lado sem explicação. Agora os dois — e a Distribuição — derivam de
 * `dataset`, que é o que os gráficos plotam. Se as contagens divergirem, é bug, não recorte.
 *
 * Resoluções não classificadas (ordem -1) já ficaram fora de `dataset.pontos`: "não sei" não é
 * uma complexidade menor que O(1), e incluí-las puxaria a mediana para baixo. Elas continuam
 * CONTADAS (e explicadas) no rodapé da Distribuição.
 */
function medianaDeK(dataset: DatasetCarta): ClasseK | null {
  if (dataset.pontos.length === 0) return null
  const ks = dataset.pontos.map((p) => p.k).sort((a, b) => a - b)
  // Mediana inferior: é sempre uma classe REAL que o aluno de fato escreveu (nunca uma média).
  return ks[Math.floor((ks.length - 1) / 2)]
}

/** Decimal em pt-BR: `3.8` → `3,8`. */
function decimal(n: number): string {
  return n.toFixed(1).replace('.', ',')
}

/**
 * Textos dos "?" do dashboard. O de complexidade explica de propósito que o card (mediana de
 * todo o histórico) e a linha do gráfico (média por período) são estatísticas diferentes —
 * por isso podem mostrar classes Big O diferentes.
 * O "?" de cada gráfico é do próprio `PainelDeGraficos`.
 */
const DASH_INFO = {
  complexidade: {
    titulo: 'Complexidade típica',
    subtitulo: 'Mediana do seu histórico analisado',
    secoes: [
      {
        rotulo: 'O que é',
        texto:
          'A classe Big O de tempo que fica no meio de todas as suas resoluções plotadas (mediana). Por ser a mediana, é sempre uma classe real que você de fato escreveu — nunca uma média inventada entre duas classes.',
      },
      {
        rotulo: 'Por que a mediana',
        texto:
          'A mediana é robusta a extremos: uma ou outra solução muito custosa (ex.: O(n²)) não distorce o valor típico, ao contrário de uma média.',
      },
      {
        rotulo: '≈ Estimado, não medido',
        texto:
          'A classe de tempo vem de análise estática da árvore sintática (AST) — é uma estimativa (determinar a complexidade exata de um código qualquer é indecidível no caso geral), e por isso o valor leva o prefixo ≈ e o marcador vazado. Só existe para Java. Confiança alta do motor não muda isso: continua sendo estimativa.',
      },
      {
        rotulo: 'Difere do gráfico "Linha"',
        texto:
          'A Linha mostra a média das classes MÊS A MÊS; este card mostra a mediana de TODO o histórico. Mesma fonte de dados (as resoluções plotadas), estatísticas diferentes — por isso podem apontar classes diferentes. Não é erro.',
      },
      {
        rotulo: 'O que não entra na conta',
        texto:
          'Resoluções ainda calculando, em linguagem sem analisador, ou que o motor não conseguiu classificar não entram na mediana — "não sei" não é uma complexidade. Elas aparecem contadas no rodapé da Distribuição, ao lado.',
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
          'Quanto maior a média, mais autônomo você declarou ter sido no conjunto das suas soluções. É um dado autodeclarado — não é medido no código, e independe da linguagem.',
      },
      {
        rotulo: 'Tendência',
        texto:
          'A legenda "tendência de alta/queda/estável" compara a autonomia das primeiras e das últimas resoluções — um sinal do seu amadurecimento ao longo do tempo.',
      },
    ],
  },
}

const TENDENCIA_LABEL: Record<Tendencia, string> = {
  alta: 'tendência de alta',
  baixa: 'tendência de queda',
  estavel: 'tendência estável',
  insuficiente: 'sua média atual',
}

// ════════════════════════════════════════════════════════════════════════════
// PEÇAS
// ════════════════════════════════════════════════════════════════════════════

const ROTULO = 'font-mono text-[11px] uppercase tracking-[.08em] text-mid'
const NOTA = 'font-mono text-[10.5px] text-soft'

/** Um dos 3 stats (spec 04 §2.2): rótulo mono + ícone · valor mono 33/600 · legenda mono. */
function StatCard({
  rotulo,
  icon: Icon,
  info,
  query,
  valor,
  legenda,
}: {
  rotulo: string
  icon: LucideIcon
  info?: React.ReactNode
  query: ResumoQuery
  valor: (r: ResumoDashboardDTO) => React.ReactNode
  legenda: (r: ResumoDashboardDTO) => React.ReactNode
}) {
  return (
    <Card className="flex flex-col gap-[9px] p-[17px]">
      <div className="flex items-center justify-between gap-2">
        <div className="flex min-w-0 items-center gap-1.5">
          <span className={ROTULO}>{rotulo}</span>
          {info}
        </div>
        <Icon size={16} strokeWidth={2} className="shrink-0 text-soft" aria-hidden />
      </div>

      {query.isPending ? (
        <>
          <Skeleton className="h-[33px] w-20" />
          <Skeleton className="h-[11px] w-32" />
        </>
      ) : query.isError ? (
        <>
          <ValorAusente />
          {/* Erro sem saída não é estado, é beco: todo erro oferece o caminho de volta. */}
          <BotaoTentarNovamente onClick={() => void query.refetch()} />
        </>
      ) : (
        <>
          {valor(query.data)}
          <span className={cn(NOTA, 'tabular')}>{legenda(query.data)}</span>
        </>
      )}
    </Card>
  )
}

/** Retry compacto dos cards do dashboard (o `ErrorState` cheio não cabe num stat de 33px). */
function BotaoTentarNovamente({ onClick }: { onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="ci-foco-botao inline-flex w-fit cursor-pointer items-center gap-1.5 rounded-ci font-mono text-[10.5px] text-erro-texto transition-colors hover:text-ink"
    >
      <RefreshCw size={11} strokeWidth={2} aria-hidden />
      não foi possível carregar · tentar novamente
    </button>
  )
}

/** Valor de stat: mono 33/600, tabular. */
function Valor({ children }: { children: React.ReactNode }) {
  return (
    <span className="tabular font-mono text-[33px] font-semibold leading-none text-ink">
      {children}
    </span>
  )
}

/** Ausência de dado — nunca um zero fingindo ser dado. */
function ValorAusente() {
  return <span className="font-mono text-[33px] font-semibold leading-none text-soft">—</span>
}

interface CartaQueryProps {
  dataset: DatasetCarta
  carregando: boolean
  erro: boolean
  onTentarNovamente: () => void
  className?: string
}

/**
 * Complexidade típica: MEDIANA das classes plotáveis + barra do colormap.
 * Big O de tempo é ESTIMADO por natureza (AST) → prefixo `≈` + `ConfidenceChip` vazado.
 * Fonte: o mesmo `dataset` dos gráficos — nunca uma segunda estatística paralela.
 */
function ComplexidadeTipicaCard({
  dataset,
  carregando,
  erro,
  onTentarNovamente,
}: CartaQueryProps) {
  const { theme } = useTheme()
  const mediana = medianaDeK(dataset)

  return (
    <Card className="flex flex-col gap-[11px] p-[17px]">
      <div className="flex items-center justify-between gap-2">
        <div className="flex min-w-0 items-center gap-1.5">
          <span className={ROTULO}>Complexidade típica</span>
          <InfoButton {...DASH_INFO.complexidade} ariaLabel="O que é a complexidade típica?" />
        </div>
        {mediana != null && !carregando && !erro && (
          <ConfidenceChip tipo={CONFIANCA_BIG_O} compact />
        )}
      </div>

      {carregando ? (
        <>
          <Skeleton className="h-[27px] w-28" />
          <Skeleton className="h-2 w-full" />
          <Skeleton className="h-[11px] w-40" />
        </>
      ) : erro ? (
        <>
          <span className="font-mono text-[27px] font-semibold leading-none text-soft">—</span>
          <BarraColormap k={null} size="card" />
          <BotaoTentarNovamente onClick={onTentarNovamente} />
        </>
      ) : mediana == null ? (
        <>
          <span className="font-mono text-[27px] font-semibold leading-none text-soft">—</span>
          <BarraColormap k={null} size="card" />
          <span className={cn(NOTA, 'leading-[1.5]')}>
            Sem análises ainda. {NOTA_METRICAS_SO_JAVA}
          </span>
        </>
      ) : (
        <>
          <span
            className="font-mono text-[27px] font-semibold leading-none"
            style={{ color: tintaDaClasse(mediana, theme) }}
          >
            {comPrefixoEstimado(rotuloCanonico(mediana), CONFIANCA_BIG_O)}
          </span>
          <BarraColormap k={mediana} size="card" />
          <span className={NOTA}>mediana das {dataset.pontos.length} resoluções plotadas</span>
        </>
      )}
    </Card>
  )
}

/** Uma linha do espectro: classe · trilho · contagem. */
function LinhaEspectro({
  rotulo,
  contagem,
  largura,
  cor,
}: {
  rotulo: string
  contagem: number
  largura: number
  cor?: string
}) {
  return (
    <div className="flex items-center gap-2.5">
      <span
        className={cn(
          'w-[52px] shrink-0 text-right font-mono text-[11px]',
          contagem > 0 ? 'text-mid' : 'text-soft',
        )}
      >
        {rotulo}
      </span>
      <div className="h-2 flex-1 overflow-hidden rounded-ci-sm bg-recess">
        {/* Contagem 0 = trilho vazio. Uma barra de 6px para zero resoluções seria dado falso. */}
        {contagem > 0 && (
          <div
            className={cn('h-full', !cor && 'bg-line-strong')}
            style={{ width: `${largura}%`, minWidth: 2, background: cor }}
          />
        )}
      </div>
      <span className="tabular w-3.5 shrink-0 text-right font-mono text-[11px] text-soft">
        {contagem}
      </span>
    </div>
  )
}

/**
 * Distribuição · Espectro (spec 04 §2.3): contagem por classe. É o ÚNICO espectro da tela — a
 * vista homônima saiu do seletor do `PainelDeGraficos` justamente porque duplicava este card.
 *
 * ⚠ MESMA FONTE do painel de gráficos (`dataset` — `linhasEspectro(dataset.pontos)`), e não
 * `resumo.distribuicaoBigO`: as duas projeções do mesmo dado tinham populações diferentes e
 * apareciam lado a lado, no mesmo viewport, com contagens que não batiam e nenhuma explicação.
 * Agora batem por construção (a soma das linhas == `dataset.pontos.length`).
 * O `?` (não classificadas) vira uma linha extra, e o rodapé conta e EXPLICA o que ficou de fora.
 */
function DistribuicaoCard({
  dataset,
  carregando,
  erro,
  onTentarNovamente,
  className,
}: CartaQueryProps) {
  const { theme } = useTheme()
  const linhas = linhasEspectro(dataset.pontos)
  const { naoClassificado, semAnalisador, calculando } = dataset.semMetrica

  return (
    <Card className={cn('flex flex-col gap-[13px] p-[17px]', className)}>
      <span className={ROTULO}>Distribuição · Espectro</span>

      {carregando ? (
        <div className="flex flex-col gap-[9px]">
          {[0, 1, 2, 3, 4].map((k) => (
            <Skeleton key={k} className="h-2 w-full" />
          ))}
        </div>
      ) : erro ? (
        <BotaoTentarNovamente onClick={onTentarNovamente} />
      ) : dataset.total === 0 ? (
        <span className="text-[12.5px] leading-[1.5] text-soft">
          Sem análises ainda — submeta resoluções em Java para ver a distribuição.
        </span>
      ) : (
        <>
          <div className="flex flex-col gap-[9px]">
            {linhas.map(({ k, curto, contagem, largura }) => (
              <LinhaEspectro
                key={k}
                rotulo={curto}
                contagem={contagem}
                largura={largura}
                cor={corDaClasse(k, theme)}
              />
            ))}

            {/* `?` = o motor rodou e não classificou. Neutro: não é uma classe do colormap. */}
            {naoClassificado > 0 && (
              <LinhaEspectro
                rotulo={ROTULO_DESCONHECIDO}
                contagem={naoClassificado}
                largura={100}
              />
            )}
          </div>

          {/* Rodapé da honestidade: o descarte é contado E explicado (§4.4). */}
          {(semAnalisador > 0 || calculando > 0) && (
            <span className={cn(NOTA, 'leading-[1.5]')}>
              {[
                calculando > 0 ? `${calculando} calculando` : null,
                semAnalisador > 0 ? `${semAnalisador} sem analisador` : null,
              ]
                .filter(Boolean)
                .join(' · ')}
              {semAnalisador > 0 ? ` · ${NOTA_METRICAS_SO_JAVA}` : ''}
            </span>
          )}
        </>
      )}
    </Card>
  )
}

/** Atividade recente (spec 04 §2.4): últimas resoluções submetidas. */
function AtividadeRecenteCard({ query }: { query: ResumoQuery }) {
  const itens = query.data?.atividadeRecente ?? []

  return (
    <Card className="flex flex-col overflow-hidden p-0">
      <div className="flex items-center justify-between gap-3 px-4 pb-[11px] pt-3.5">
        <h3 className="text-[13.5px] font-semibold text-ink">Atividade recente</h3>
        <Link
          to="/app/desafios"
          className="ci-foco-botao rounded-ci px-1 py-0.5 font-mono text-[11px] text-steel transition-colors hover:text-steel-hover"
        >
          ver tudo
        </Link>
      </div>

      {query.isPending ? (
        <div className="flex flex-col gap-2 px-4 pb-4">
          {[0, 1, 2, 3].map((k) => (
            <Skeleton key={k} className="h-9 w-full" />
          ))}
        </div>
      ) : query.isError ? (
        <div className="px-4 pb-4">
          <ErrorState
            message={apiErrorMessage(query.error)}
            onRetry={() => void query.refetch()}
          />
        </div>
      ) : itens.length === 0 ? (
        <EmptyState
          icon={Code2}
          size="sm"
          title="Nenhuma resolução ainda."
          description="Registre seu primeiro desafio para começar a carta."
          action={
            <Link to="/app/desafios" className={buttonClasses({ size: 'sm' })}>
              <Plus size={14} strokeWidth={2} aria-hidden />
              Novo desafio
            </Link>
          }
          className="pb-6"
        />
      ) : (
        <div className="flex flex-col">
          {itens.map((a) => (
            <AtividadeLinha
              key={a.resolucaoId}
              to={`/app/resolucoes/${a.resolucaoId}`}
              titulo={a.desafioTitulo}
              linguagem={a.linguagem}
              autonomia={a.indiceAutonomiaIA}
              analisada={a.analisada}
              tempoOrdem={a.complexidadeOrdem}
              submetidaEm={a.submetidaEm}
              dataFormato="longa"
            />
          ))}
        </div>
      )}
    </Card>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// TELA
// ════════════════════════════════════════════════════════════════════════════

export function DashboardPage() {
  const { user } = useAuth()
  const resumoQuery = useResumoDashboard()
  const cartaQuery = useCartaCeleste()

  const dataset = useMemo(() => montarDataset(cartaQuery.data ?? []), [cartaQuery.data])
  const [selecionadoId, setSelecionadoId] = useState<string | null>(null)
  const selecionado = pontoPorId(dataset, selecionadoId)

  /*
   * As duas LENTES do painel de gráficos moram AQUI, e não dentro dele: o `PainelEstrelaSelecionada`
   * ao lado precisa das duas para saber quem são as IRMÃS da resolução selecionada — os gráficos
   * agrupam por critérios diferentes (célula autonomia × classe na Carta e na Matriz; bucket do
   * tempo na Linha, cujo tamanho depende da granularidade). A visualização vai para a URL (é
   * compartilhável); a granularidade não (é uma lente sobre o mesmo dado, não outra tela).
   */
  const [view, setView] = useGraficoNaUrl()
  const [granularidade, setGranularidade] = useState<Granularidade>(GRANULARIDADE_PADRAO)

  const grupo = useMemo(
    () => grupoDeIrmaos({ view, granularidade, dataset, selecionadoId }),
    [view, granularidade, dataset, selecionadoId],
  )

  /** Clicar na mesma estrela desfaz a seleção. */
  const selecionar = (resolucaoId: string) =>
    setSelecionadoId((atual) => (atual === resolucaoId ? null : resolucaoId))

  return (
    <PageContainer>
      <PageHeader
        title={`Olá, ${user?.username ?? 'aluno'}`}
        subtitle="Aqui está a carta do seu portfólio."
        actions={
          <>
            <Link to="/app/snippets" className={buttonClasses({ variant: 'secondary', size: 'sm' })}>
              <Braces size={14} strokeWidth={2} aria-hidden />
              Novo snippet
            </Link>
            <Link to="/app/desafios" className={buttonClasses({ size: 'sm' })}>
              <Plus size={14} strokeWidth={2} aria-hidden />
              Novo desafio
            </Link>
          </>
        }
      />

      {/* ── 3 stats ─────────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-3">
        <StatCard
          rotulo="Desafios"
          icon={Target}
          query={resumoQuery}
          valor={(r) => <Valor>{r.totalDesafios}</Valor>}
          legenda={(r) =>
            `${pluralPt(r.desafiosPublicos, 'público', 'públicos')} · ${pluralPt(
              r.totalDesafios - r.desafiosPublicos,
              'privado',
              'privados',
            )}`
          }
        />

        <StatCard
          rotulo="Resoluções"
          icon={Code2}
          query={resumoQuery}
          valor={(r) => <Valor>{r.totalResolucoes}</Valor>}
          legenda={(r) => {
            const calculando = r.totalResolucoes - r.resolucoesAnalisadas
            return (
              <>
                {pluralPt(r.resolucoesAnalisadas, 'analisada', 'analisadas')}
                {calculando > 0 && (
                  <>
                    {' · '}
                    <span className="text-ink">{calculando} calculando</span>
                  </>
                )}
              </>
            )
          }}
        />

        <StatCard
          rotulo="Autonomia média"
          icon={Gauge}
          query={resumoQuery}
          info={
            <InfoButton {...DASH_INFO.autonomia} ariaLabel="O que é a Autonomia IA média?" />
          }
          valor={(r) =>
            r.mediaAutonomia == null ? (
              <ValorAusente />
            ) : (
              <div className="flex items-baseline gap-[3px]">
                <Valor>{decimal(r.mediaAutonomia)}</Valor>
                <span className="font-mono text-[15px] text-soft">/5</span>
              </div>
            )
          }
          legenda={(r) =>
            r.mediaAutonomia == null
              ? 'sem resoluções ainda'
              : TENDENCIA_LABEL[tendencia(r.evolucao.map((p) => p.mediaAutonomia))]
          }
        />
      </div>

      {/* ── Herói: painel de gráficos (Carta · Linha · Matriz) + coluna lateral ── */}
      <div className="grid grid-cols-1 gap-3.5 lg:grid-cols-[1.62fr_1fr]">
        <PainelDeGraficos
          dataset={dataset}
          carregando={cartaQuery.isPending}
          erro={cartaQuery.isError ? apiErrorMessage(cartaQuery.error) : null}
          onTentarNovamente={() => void cartaQuery.refetch()}
          selecionadoId={selecionadoId}
          onSelecionar={selecionar}
          view={view}
          onViewChange={setView}
          granularidade={granularidade}
          onGranularidadeChange={setGranularidade}
        />

        <div className="flex min-w-0 flex-col gap-3.5">
          {/*
           * `grupo` + `onSelecionar`: os três gráficos colapsam num único alvo as resoluções que
           * caem juntas (mesma célula, na Carta e na Matriz; mesmo período, na Linha) e o clique
           * abre só UMA delas — é este painel que devolve as irmãs ao alcance do usuário, com o
           * navegador "‹ 2 de 3 ›". O grupo depende do gráfico que está na tela: por isso vem de
           * `grupoDeIrmaos(...)`, e não de um agrupamento fixo.
           */}
          <PainelEstrelaSelecionada
            ponto={selecionado}
            grupo={grupo}
            onSelecionar={selecionar}
          />
          <ComplexidadeTipicaCard
            dataset={dataset}
            carregando={cartaQuery.isPending}
            erro={cartaQuery.isError}
            onTentarNovamente={() => void cartaQuery.refetch()}
          />
          <DistribuicaoCard
            dataset={dataset}
            carregando={cartaQuery.isPending}
            erro={cartaQuery.isError}
            onTentarNovamente={() => void cartaQuery.refetch()}
            className="flex-1"
          />
        </div>
      </div>

      {/* ── Atividade recente ───────────────────────────────────────────────── */}
      <AtividadeRecenteCard query={resumoQuery} />
    </PageContainer>
  )
}
