import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { AlertTriangle, Globe, Info, Plus, Sparkles, Trash2 } from 'lucide-react'
import {
  useAlterarVisibilidadeResolucao,
  useRemoverResolucao,
  useResolucaoDetalhe,
} from '@/features/resolucoes/hooks'
import { useDesafioDetalhe } from '@/features/desafios/hooks'
import { useMetricasDaResolucao } from '@/features/metricas/hooks'
import { PageContainer } from '@/components/page/PageContainer'
import { Breadcrumb } from '@/components/page/Breadcrumb'
import { ErrorState, QueryBoundary } from '@/components/page/states'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { EmptyState } from '@/components/ui/empty-state'
import { Skeleton } from '@/components/ui/skeleton'
import { Spinner } from '@/components/ui/spinner'
import { ConfirmDialog } from '@/components/ui/confirm-dialog'
import { toast } from '@/components/ui/toaster'
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
import type { ResolucaoDetalheDTO, ResultadoMetricaDTO, TipoMetrica } from '@/types/api'
import { formatDateTime } from '@/lib/utils'
import { apiErrorMessage } from '@/lib/api'

/** Ordem canônica das 3 métricas de AST no retrato. */
const ORDEM_METRICAS: TipoMetrica[] = [
  'COMPLEXIDADE_CICLOMATICA',
  'BIG_O_TEMPO',
  'COMPLEXIDADE_ESPACO',
]

/**
 * Detalhe de uma resolução do aluno + retrato de métricas.
 * Rota: /app/resolucoes/:resolucaoId — renderiza dentro do AppLayout (via Outlet).
 * A análise de complexidade é assíncrona (só Java hoje): enquanto `analisada`
 * é false mostramos "Calculando…" e, ao virar true, rebuscamos as métricas.
 */
export function ResolucaoDetalhePage() {
  const { resolucaoId } = useParams()
  const navigate = useNavigate()

  const resolucaoQuery = useResolucaoDetalhe(resolucaoId)
  const resolucao = resolucaoQuery.data

  // Título do desafio: cruza ResolucaoDetalheDTO.desafioId com o detalhe do desafio
  // (o DTO da resolução não traz o título). Pode estar carregando/undefined.
  const desafioQuery = useDesafioDetalhe(resolucao?.desafioId)
  const tituloDesafio = desafioQuery.data?.titulo

  const metricasQuery = useMetricasDaResolucao(resolucaoId)

  // Quando a análise assíncrona conclui (analisada: false → true), rebusca as métricas.
  const analisada = resolucao?.analisada
  useEffect(() => {
    if (analisada) void metricasQuery.refetch()
  }, [analisada, metricasQuery.refetch])

  const remover = useRemoverResolucao()
  const alterarVisibilidade = useAlterarVisibilidadeResolucao(resolucaoId ?? '')

  const [removerOpen, setRemoverOpen] = useState(false)
  const [visibilidadeOpen, setVisibilidadeOpen] = useState(false)

  async function confirmarRemocao(r: ResolucaoDetalheDTO) {
    try {
      await remover.mutateAsync(r.id)
      toast.success('Resolução removida.')
      navigate(`/app/desafios/${r.desafioId}`)
    } catch (e) {
      toast.error(apiErrorMessage(e))
    }
  }

  async function confirmarVisibilidade(r: ResolucaoDetalheDTO) {
    try {
      await alterarVisibilidade.mutateAsync(r.visibilidade !== 'PUBLICO')
      toast.success(
        r.visibilidade === 'PUBLICO' ? 'Resolução tornada privada.' : 'Resolução publicada.',
      )
      setVisibilidadeOpen(false)
    } catch (e) {
      toast.error(apiErrorMessage(e))
    }
  }

  return (
    <PageContainer>
      <QueryBoundary query={resolucaoQuery}>
        {(resolucao) => {
          const metricas: ResultadoMetricaDTO[] = metricasQuery.data ?? []
          const porTipo = new Map(metricas.map((m) => [m.tipo, m]))
          const semMetricas = metricas.length === 0
          const naoJava = resolucao.linguagem !== 'JAVA'
          const ehPublica = resolucao.visibilidade === 'PUBLICO'

          return (
            <>
              <Breadcrumb
                items={[
                  { label: 'Desafios', to: '/app/desafios' },
                  {
                    label: tituloDesafio ?? 'Desafio',
                    to: `/app/desafios/${resolucao.desafioId}`,
                  },
                  { label: 'Resolução' },
                ]}
              />

              {/* Cabeçalho */}
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="flex min-w-0 flex-col gap-2.5">
                  <h1 className="text-[25px] font-bold tracking-tight text-heading">
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
                <div className="flex flex-wrap gap-2">
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => navigate(`/app/desafios/${resolucao.desafioId}/submeter`)}
                  >
                    <Plus size={15} />
                    Nova resolução
                  </Button>
                  <Button variant="destructive" size="sm" onClick={() => setRemoverOpen(true)}>
                    <Trash2 size={15} />
                    Remover
                  </Button>
                </div>
              </div>

              {/* Retrato de métricas (faixa) */}
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
                          {LINGUAGEM_META[resolucao.linguagem].label} — hoje o motor analisa apenas
                          Java.
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
                            tipo === 'COMPLEXIDADE_CICLOMATICA'
                              ? undefined
                              : complexityHexByOrdinal(m.valor)
                          return (
                            <MetricCard
                              key={tipo}
                              tipo={tipo}
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

                  {/* Autonomia (autodeclarada, não vem do motor) */}
                  <div className="flex flex-col justify-center gap-2.5 rounded-xl border border-border bg-surface p-4">
                    <span className="text-[12.5px] font-semibold text-muted">Autonomia IA</span>
                    <AutonomyMeter value={resolucao.indiceAutonomiaIA} size="md" />
                    <span className="text-[11px] text-subtle">autodeclarada</span>
                  </div>
                </div>

                {/* Nota de honestidade científica */}
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
                      <p className="text-[13px] leading-relaxed text-fg">
                        {resolucao.descricaoApoioIA}
                      </p>
                    </Card>
                  )}

                  <Card className="flex flex-col gap-3 p-4">
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-[12.5px] text-muted">Visibilidade</span>
                      <VisibilityBadge visibilidade={resolucao.visibilidade} />
                    </div>
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-[12.5px] text-muted">Linguagem</span>
                      <LanguageBadge linguagem={resolucao.linguagem} />
                    </div>
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-[12.5px] text-muted">Enviada em</span>
                      <span className="font-mono text-[12px] text-fg">
                        {formatDateTime(resolucao.submetidaEm)}
                      </span>
                    </div>
                    <Button
                      variant="secondary"
                      size="sm"
                      className="mt-1 w-full"
                      onClick={() => setVisibilidadeOpen(true)}
                    >
                      <Globe size={15} />
                      {ehPublica ? 'Tornar privada' : 'Publicar resolução'}
                    </Button>
                    <p className="text-[11px] leading-relaxed text-subtle">
                      {ehPublica
                        ? 'Visível a visitantes no portfólio público (quando o desafio é público).'
                        : 'Privada: só você vê. Publique para exibi-la no portfólio.'}
                    </p>
                  </Card>

                  <div className="flex gap-2.5 rounded-xl border border-border bg-surface p-3.5">
                    <Sparkles size={15} className="mt-0.5 shrink-0 text-brand-strong" />
                    <span className="text-[11.5px] leading-relaxed text-muted">
                      Melhorou a solução? Envie uma{' '}
                      <button
                        type="button"
                        className="font-semibold text-brand-strong underline-offset-2 hover:underline"
                        onClick={() =>
                          navigate(`/app/desafios/${resolucao.desafioId}/submeter`)
                        }
                      >
                        nova resolução
                      </button>{' '}
                      — cada envio vira um ponto na sua evolução, sem apagar esta.
                    </span>
                  </div>
                </div>
              </div>

              {/* Diálogo: alterar visibilidade */}
              <ConfirmDialog
                open={visibilidadeOpen}
                onOpenChange={setVisibilidadeOpen}
                icon={Globe}
                title={ehPublica ? 'Tornar privada?' : 'Publicar resolução?'}
                description={
                  ehPublica
                    ? 'Ela deixará de aparecer no portfólio público deste desafio.'
                    : 'Ela ficará visível para visitantes no portfólio público (quando o desafio for público).'
                }
                confirmLabel={ehPublica ? 'Tornar privada' : 'Publicar'}
                loading={alterarVisibilidade.isPending}
                onConfirm={() => void confirmarVisibilidade(resolucao)}
              />

              {/* Diálogo: remover resolução */}
              <ConfirmDialog
                open={removerOpen}
                onOpenChange={setRemoverOpen}
                icon={Trash2}
                title="Remover resolução"
                description="Esta ação é permanente: a resolução e suas métricas serão apagadas."
                confirmLabel="Remover"
                destructive
                loading={remover.isPending}
                onConfirm={() => void confirmarRemocao(resolucao)}
              />
            </>
          )
        }}
      </QueryBoundary>
    </PageContainer>
  )
}

/** Estado assíncrono: banner + esqueletos enquanto o motor analisa. */
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
          <div
            key={nome}
            className="flex flex-col gap-3 rounded-xl border border-border bg-surface p-4"
          >
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
