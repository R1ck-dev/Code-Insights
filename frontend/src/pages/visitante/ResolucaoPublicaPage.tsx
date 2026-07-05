import { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AlertTriangle, ArrowLeft, Eye, Info, Sparkles } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { EmptyState } from '@/components/ui/empty-state'
import { buttonClasses } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Spinner } from '@/components/ui/spinner'
import { LoadingSection, ErrorState } from '@/components/page/states'
import { CodeBlock } from '@/components/CodeBlock'
import { MetricCard } from '@/components/MetricCard'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { AnalysisStatus, LanguageBadge, VisibilityBadge } from '@/components/domain/badges'
import {
  complexityHexByOrdinal,
  LINGUAGEM_META,
  prettyBigO,
  TIPO_METRICA_META,
} from '@/domain/enums'
import { useResolucaoDetalhe } from '@/features/resolucoes/hooks'
import { useDesafioDetalhe } from '@/features/desafios/hooks'
import { useMetricasDaResolucao } from '@/features/metricas/hooks'
import type { ResultadoMetricaDTO, TipoMetrica } from '@/types/api'
import { formatDateTime } from '@/lib/utils'
import { apiErrorMessage, apiErrorStatus } from '@/lib/api'

const ORDEM_METRICAS: TipoMetrica[] = [
  'COMPLEXIDADE_CICLOMATICA',
  'BIG_O_TEMPO',
  'COMPLEXIDADE_ESPACO',
]

/**
 * Visitante — resolução pública em modo leitura (sem editar/remover/publicar).
 * Rota: /u/:usuarioId/desafios/:desafioId/resolucoes/:resolucaoId (dentro do PublicLayout).
 * O backend só entrega resolução pública sob desafio público (senão 400/401).
 */
export function ResolucaoPublicaPage() {
  const { usuarioId, desafioId, resolucaoId } = useParams()
  const resolucaoQuery = useResolucaoDetalhe(resolucaoId)
  const resolucao = resolucaoQuery.data

  const desafioQuery = useDesafioDetalhe(desafioId)
  const tituloDesafio = desafioQuery.data?.titulo

  const metricasQuery = useMetricasDaResolucao(resolucaoId)
  const analisada = resolucao?.analisada
  useEffect(() => {
    if (analisada) void metricasQuery.refetch()
  }, [analisada, metricasQuery.refetch])

  const desafioTo = `/u/${usuarioId ?? ''}/desafios/${desafioId ?? ''}`

  if (resolucaoQuery.isPending) {
    return (
      <div className="mx-auto w-full max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
        <LoadingSection />
      </div>
    )
  }

  // Lacuna da API: sem login, abrir a resolução pode dar 401. Diferencia o motivo.
  if (resolucaoQuery.isError || !resolucao) {
    const precisaLogin = apiErrorStatus(resolucaoQuery.error) === 401
    const msg = precisaLogin
      ? 'Entre para ver esta resolução.'
      : apiErrorMessage(resolucaoQuery.error, 'Não foi possível carregar esta resolução.')
    return (
      <div className="mx-auto flex w-full max-w-lg flex-col items-center gap-5 px-4 py-16 sm:px-6 lg:px-8">
        <ErrorState message={msg} className="w-full" />
        <div className="flex items-center gap-3">
          {precisaLogin ? (
            <Link to="/entrar" className={buttonClasses({ variant: 'primary' })}>
              Entrar
            </Link>
          ) : null}
          <Link to={desafioTo} className={buttonClasses({ variant: 'secondary' })}>
            Voltar ao desafio
          </Link>
        </div>
      </div>
    )
  }

  const metricas: ResultadoMetricaDTO[] = metricasQuery.data ?? []
  const porTipo = new Map(metricas.map((m) => [m.tipo, m]))
  const semMetricas = metricas.length === 0
  const naoJava = resolucao.linguagem !== 'JAVA'

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-6 px-4 py-8 sm:px-6 lg:px-8">
      <Link
        to={desafioTo}
        className="inline-flex w-fit items-center gap-2 text-[13px] font-medium text-brand-strong transition-colors hover:text-brand"
      >
        <ArrowLeft size={15} />
        {tituloDesafio ? `Voltar para ${tituloDesafio}` : 'Voltar ao desafio'}
      </Link>

      {/* Cabeçalho */}
      <div className="flex min-w-0 flex-col gap-2.5">
        <h1 className="text-2xl font-bold tracking-tight text-heading">
          {tituloDesafio ?? 'Resolução'}
        </h1>
        <div className="flex flex-wrap items-center gap-2.5">
          <LanguageBadge linguagem={resolucao.linguagem} />
          <VisibilityBadge visibilidade={resolucao.visibilidade} />
          <span className="font-mono text-[11.5px] text-subtle">
            enviada em {formatDateTime(resolucao.submetidaEm)}
          </span>
        </div>
      </div>

      {/* Retrato de métricas */}
      <Card className="flex flex-col gap-3.5 bg-surface-2 p-4 md:p-5">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <span className="text-[14px] font-bold text-heading">Retrato de métricas</span>
          <AnalysisStatus analisada={resolucao.analisada} />
        </div>

        <div className="grid gap-3 md:grid-cols-4">
          <div className="md:col-span-3">
            {!resolucao.analisada ? (
              <CalculandoMetricas />
            ) : metricasQuery.isError ? (
              <ErrorState
                message={apiErrorMessage(metricasQuery.error)}
                onRetry={() => void metricasQuery.refetch()}
              />
            ) : semMetricas && metricasQuery.isFetching ? (
              <CalculandoMetricas />
            ) : semMetricas && naoJava ? (
              <div className="flex items-start gap-3 rounded-[11px] border border-warning/25 bg-warning/[.06] px-4 py-4">
                <AlertTriangle size={17} className="mt-px shrink-0 text-warning" />
                <span className="text-[13px] leading-relaxed text-muted">
                  Análise de complexidade indisponível para{' '}
                  {LINGUAGEM_META[resolucao.linguagem].label} — hoje o motor analisa apenas Java.
                </span>
              </div>
            ) : semMetricas ? (
              <EmptyState
                icon={Info}
                title="Sem métricas"
                description="A análise concluiu, mas não retornou métricas para esta resolução."
              />
            ) : (
              <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
                {ORDEM_METRICAS.map((tipo) => {
                  const m = porTipo.get(tipo)
                  if (!m) return null
                  const meta = TIPO_METRICA_META[tipo]
                  const valueColor =
                    tipo === 'COMPLEXIDADE_CICLOMATICA' ? undefined : complexityHexByOrdinal(m.valor)
                  return (
                    <MetricCard
                      key={tipo}
                      nome={meta.nome}
                      sub={meta.sub}
                      rotulo={prettyBigO(m.rotulo)}
                      natureza={meta.natureza}
                      valueColor={valueColor}
                      detalhe={m.detalhe}
                    />
                  )
                })}
              </div>
            )}
          </div>

          <div className="flex flex-col justify-center gap-2.5 rounded-xl border border-border bg-surface p-4">
            <span className="text-[12.5px] font-semibold text-muted">Autonomia IA</span>
            <AutonomyMeter value={resolucao.indiceAutonomiaIA} size="md" />
            <span className="text-[11px] text-subtle">autodeclarada</span>
          </div>
        </div>

        <p className="text-[11.5px] leading-relaxed text-subtle">
          <span className="font-semibold text-muted">Exata</span> = contagem direta no AST.{' '}
          <span className="font-semibold text-muted">Estimada</span> = heurística.
        </p>
      </Card>

      {/* Código + coluna lateral */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1.6fr_1fr] lg:items-start">
        <CodeBlock
          code={resolucao.codigoFonte}
          lang={LINGUAGEM_META[resolucao.linguagem].codeLang}
          label="Solution"
          maxHeight={460}
        />

        <div className="flex flex-col gap-3.5">
          {resolucao.descricaoApoioIA && (
            <Card className="flex flex-col gap-2 p-4">
              <span className="flex items-center gap-1.5 text-[13px] font-semibold text-muted">
                <Sparkles size={14} className="text-brand-strong" />
                Como a IA ajudou
              </span>
              <p className="text-[13px] leading-relaxed text-fg">{resolucao.descricaoApoioIA}</p>
            </Card>
          )}

          <div className="flex items-start gap-2.5 rounded-xl border border-border bg-bg-deep p-3.5">
            <Eye size={15} className="mt-0.5 shrink-0 text-brand-strong" />
            <span className="text-xs leading-relaxed text-muted">
              Você está vendo em modo leitura.{' '}
              <Link to="/criar-conta" className="font-semibold text-brand-strong hover:text-brand">
                Crie uma conta
              </Link>{' '}
              para montar o seu.
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}

function CalculandoMetricas() {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-3 rounded-[11px] border border-info/25 bg-info/[.07] px-4 py-3.5">
        <Spinner size={20} color="var(--info)" />
        <div className="flex flex-col gap-0.5">
          <span className="text-[13.5px] font-semibold text-info">Calculando métricas…</span>
          <span className="text-[12px] text-muted">
            A análise roda de forma assíncrona. Os cartões aparecem em alguns segundos.
          </span>
        </div>
      </div>
      <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
        {['Ciclomática', 'Tempo', 'Espaço'].map((nome) => (
          <div key={nome} className="flex flex-col gap-3 rounded-xl border border-border bg-surface p-4">
            <span className="text-[12px] font-semibold text-muted">{nome}</span>
            <Skeleton className="h-8 w-16 rounded-lg" />
            <Skeleton className="h-3 w-full rounded" />
            <Skeleton className="h-3 w-3/5 rounded" />
          </div>
        ))}
      </div>
    </div>
  )
}
