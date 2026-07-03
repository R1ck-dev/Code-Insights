import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { AlertTriangle, Info, Pencil, Sparkles, Trash2 } from 'lucide-react'
import {
  useAtualizarResolucao,
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
import { FormField } from '@/components/ui/form-field'
import {
  Dialog,
  DialogBody,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { ConfirmDialog } from '@/components/ui/confirm-dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { toast } from '@/components/ui/toaster'
import { CodeBlock } from '@/components/CodeBlock'
import { CodeEditor } from '@/components/CodeEditor'
import { MetricCard } from '@/components/MetricCard'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { AnalysisStatus, LanguageBadge, VisibilityBadge } from '@/components/domain/badges'
import {
  complexityHexByOrdinal,
  LINGUAGEM_META,
  LINGUAGENS,
  prettyBigO,
  TIPO_METRICA_META,
} from '@/domain/enums'
import type {
  LinguagemProgramacao,
  ResolucaoDetalheDTO,
  ResultadoMetricaDTO,
  TipoMetrica,
} from '@/types/api'
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

  const atualizar = useAtualizarResolucao(resolucaoId ?? '')
  const remover = useRemoverResolucao()

  // Diálogo de edição de código (pré-preenchido ao abrir).
  const [editOpen, setEditOpen] = useState(false)
  const [editCodigo, setEditCodigo] = useState('')
  const [editLinguagem, setEditLinguagem] = useState<LinguagemProgramacao>('JAVA')
  const [removerOpen, setRemoverOpen] = useState(false)

  function abrirEdicao(r: ResolucaoDetalheDTO) {
    setEditCodigo(r.codigoFonte)
    setEditLinguagem(r.linguagem)
    setEditOpen(true)
  }

  async function salvarEdicao() {
    try {
      await atualizar.mutateAsync({ codigoFonte: editCodigo, linguagem: editLinguagem })
      toast.success('Código atualizado. Recalculando as métricas…')
      setEditOpen(false)
    } catch (e) {
      toast.error(apiErrorMessage(e))
    }
  }

  async function confirmarRemocao(r: ResolucaoDetalheDTO) {
    try {
      await remover.mutateAsync(r.id)
      toast.success('Resolução removida.')
      navigate(`/app/desafios/${r.desafioId}`)
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
                  <h2 className="text-[25px] font-bold tracking-tight text-heading">
                    {tituloDesafio ?? 'Resolução'}
                  </h2>
                  <div className="flex flex-wrap items-center gap-2.5">
                    <LanguageBadge linguagem={resolucao.linguagem} />
                    <VisibilityBadge visibilidade={resolucao.visibilidade} />
                    <span className="font-mono text-[11.5px] text-subtle">
                      enviada em {formatDateTime(resolucao.submetidaEm)}
                    </span>
                  </div>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button variant="secondary" size="sm" onClick={() => abrirEdicao(resolucao)}>
                    <Pencil size={15} />
                    Editar código
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
                  </Card>

                  <div className="flex gap-2.5 rounded-xl border border-warning/25 bg-warning/[.06] p-3.5">
                    <AlertTriangle size={15} className="mt-0.5 shrink-0 text-warning" />
                    <span className="text-[11.5px] leading-relaxed text-muted">
                      Editar o código dispara nova análise e recalcula todas as métricas.
                    </span>
                  </div>
                </div>
              </div>

              {/* Diálogo: editar código */}
              <Dialog open={editOpen} onOpenChange={setEditOpen}>
                <DialogContent className="max-w-2xl">
                  <DialogHeader>
                    <div className="flex flex-col gap-1">
                      <DialogTitle>Editar código</DialogTitle>
                      <DialogDescription>
                        Editar recalcula as métricas desta resolução.
                      </DialogDescription>
                    </div>
                  </DialogHeader>
                  <DialogBody>
                    <FormField label="Linguagem" htmlFor="edit-linguagem">
                      <Select
                        value={editLinguagem}
                        onValueChange={(v) => setEditLinguagem(v as LinguagemProgramacao)}
                      >
                        <SelectTrigger id="edit-linguagem" className="w-full">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {LINGUAGENS.map((l) => (
                            <SelectItem key={l.value} value={l.value}>
                              {l.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </FormField>

                    <FormField label="Código-fonte">
                      <CodeEditor
                        value={editCodigo}
                        onChange={setEditCodigo}
                        label="Solution"
                        minHeight={280}
                      />
                    </FormField>

                    <div className="flex gap-2.5 rounded-[10px] border border-warning/25 bg-warning/[.06] p-3">
                      <AlertTriangle size={15} className="mt-0.5 shrink-0 text-warning" />
                      <span className="text-[12px] leading-relaxed text-muted">
                        Editar recalcula as métricas: a resolução volta para "calculando" e o motor
                        reanalisa o código.
                      </span>
                    </div>
                  </DialogBody>
                  <DialogFooter>
                    <DialogClose asChild>
                      <Button variant="secondary">Cancelar</Button>
                    </DialogClose>
                    <Button
                      onClick={salvarEdicao}
                      loading={atualizar.isPending}
                      disabled={!editCodigo.trim()}
                    >
                      Salvar e reanalisar
                    </Button>
                  </DialogFooter>
                </DialogContent>
              </Dialog>

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
