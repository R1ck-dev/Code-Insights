import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  AlertTriangle,
  BookOpen,
  Check,
  Cpu,
  Gauge,
  Globe,
  Lock,
  Plus,
  RefreshCw,
  Trash2,
} from 'lucide-react'
import {
  useAlterarVisibilidadeResolucao,
  useRemoverResolucao,
  useResolucaoDetalhe,
} from '@/features/resolucoes/hooks'
import { useDesafioDetalhe } from '@/features/desafios/hooks'
import { useMetricasDaResolucao } from '@/features/metricas/hooks'
import { PageContainer } from '@/components/page/PageContainer'
import { Breadcrumb } from '@/components/page/Breadcrumb'
import { QueryBoundary } from '@/components/page/states'
import { Button, buttonClasses } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { ConfirmDialog } from '@/components/ui/confirm-dialog'
import { InfoButton } from '@/components/ui/info-button'
import { toast } from '@/components/ui/toaster'
import { CodeBlock } from '@/components/CodeBlock'
import { MetricTile } from '@/components/MetricTile'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { LangChip, LanguageDot, StatusChip } from '@/components/domain/badges'
import {
  LINGUAGEM_COM_METRICAS,
  LINGUAGEM_META,
  NOTA_METRICAS_SO_JAVA,
  ROTULO_SEM_METRICA,
  TIPO_METRICA_META,
  prettyBigO,
} from '@/domain/enums'
import { METRICA_EXPLICACAO, notaDaMetrica, secaoDoMotor } from '@/domain/metricas-explicacao'
import type {
  LinguagemProgramacao,
  NivelConfianca,
  ResolucaoDetalheDTO,
  ResultadoMetricaDTO,
  TipoMetrica,
} from '@/types/api'
import { formatDate, formatDateTime } from '@/lib/utils'
import { apiErrorMessage } from '@/lib/api'

/** Ordem canônica dos 3 tiles de AST na faixa (spec 04 §3.2). */
const ORDEM_METRICAS: TipoMetrica[] = [
  'COMPLEXIDADE_CICLOMATICA',
  'BIG_O_TEMPO',
  'COMPLEXIDADE_ESPACO',
]

/** Confiança do MOTOR no próprio valor — eixo distinto de MEDIDO/≈ ESTIMADO. */
const NIVEL_CONFIANCA_LABEL: Record<NivelConfianca, string> = {
  ALTA: 'alta',
  MEDIA: 'média',
  BAIXA: 'baixa',
}

/** Nome do arquivo no cabeçalho do CodeBlock. */
const EXTENSAO: Record<LinguagemProgramacao, string> = {
  JAVA: 'java',
  PYTHON: 'py',
  CPP: 'cpp',
  JAVASCRIPT: 'js',
  C: 'c',
}

/**
 * Estado da faixa de métricas.
 * `calculando` = motor rodando · `sem-metrica` = linguagem ≠ Java (§4.4) ·
 * `vazio` = analisou e não extraiu nada (parse falhou) · `erro` = a busca falhou.
 */
type EstadoFaixa = 'calculando' | 'pronta' | 'sem-metrica' | 'vazio' | 'erro'

/**
 * D ★ · Resolução — a tela-assinatura.
 * Rota: /app/resolucoes/:resolucaoId (dentro do AppLayout).
 *
 * A análise de complexidade é assíncrona e só existe para Java: enquanto
 * `analisada` é false o hook refaz a busca a cada 4s e, ao virar true,
 * rebuscamos as métricas. A incerteza nunca é escondida — ciclomática é MEDIDO
 * (marcador cheio), tempo/espaço são ≈ ESTIMADO (marcador vazado).
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
      <QueryBoundary query={resolucaoQuery} loading={<EsqueletoResolucao />}>
        {(resolucao) => {
          const metricas: ResultadoMetricaDTO[] = metricasQuery.data ?? []
          const porTipo = new Map(metricas.map((m) => [m.tipo, m]))
          const semMetricas = metricas.length === 0
          const naoJava = resolucao.linguagem !== LINGUAGEM_COM_METRICAS
          const ehPublica = resolucao.visibilidade === 'PUBLICO'
          const submeterHref = `/app/desafios/${resolucao.desafioId}/submeter`

          const estado: EstadoFaixa = metricasQuery.isError
            ? 'erro'
            : !resolucao.analisada ||
                metricasQuery.isPending ||
                (semMetricas && metricasQuery.isFetching)
              ? 'calculando'
              : semMetricas
                ? naoJava
                  ? 'sem-metrica'
                  : 'vazio'
                : 'pronta'

          const analisadoEm = metricas[0]?.analisadoEm

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

              {/* ---------------------------------------------- cabeçalho */}
              <header className="flex flex-wrap items-start justify-between gap-5">
                <div className="flex min-w-0 flex-col gap-[9px]">
                  <h2 className="text-[25px] font-semibold leading-tight tracking-[-.02em] text-ink">
                    {tituloDesafio ?? 'Resolução'}
                  </h2>
                  <div className="flex flex-wrap items-center gap-[9px]">
                    <LangChip lang={resolucao.linguagem} />
                    <StatusChip status={ehPublica ? 'publico' : 'privado'} />
                    <span className="font-mono text-[11px] tabular-nums text-soft">
                      enviada em {formatDateTime(resolucao.submetidaEm)}
                    </span>
                  </div>
                </div>

                <div className="flex flex-wrap gap-[9px]">
                  <Link
                    to={submeterHref}
                    className={buttonClasses({ variant: 'secondary', size: 'sm' })}
                  >
                    <Plus size={14} strokeWidth={2} aria-hidden />
                    Nova resolução
                  </Link>
                  <Button
                    variant="destructive"
                    size="sm"
                    icon={Trash2}
                    onClick={() => setRemoverOpen(true)}
                  >
                    Remover
                  </Button>
                </div>
              </header>

              {/* -------------------------------- ★ faixa de métricas */}
              <Card className="flex flex-col gap-3.5 px-[18px] py-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <span className="font-mono text-[11px] uppercase tracking-[.12em] text-mid">
                    Retrato de métricas
                  </span>
                  <LinhaDeEstado
                    estado={estado}
                    linguagem={resolucao.linguagem}
                    analisadoEm={analisadoEm}
                  />
                </div>

                <div className="grid grid-cols-1 gap-[13px] sm:grid-cols-2 xl:grid-cols-[repeat(3,1fr)_0.92fr]">
                  {ORDEM_METRICAS.map((tipo) => {
                    const meta = TIPO_METRICA_META[tipo]
                    const m = porTipo.get(tipo)
                    const pronta = estado === 'pronta' && !!m

                    return (
                      <MetricTile
                        key={tipo}
                        rotulo={meta.rotulo}
                        metodo={meta.metodo}
                        valor={pronta ? valorDoTile(tipo, m) : null}
                        confianca={meta.confianca}
                        k={pronta && meta.ehClasseBigO ? m.valor : null}
                        barra={meta.ehClasseBigO}
                        calculando={estado === 'calculando'}
                        erro={estado === 'erro'}
                        nota={notaDoTile(estado, meta.ehClasseBigO, tipo, m)}
                        info={
                          <InfoButton
                            {...METRICA_EXPLICACAO[tipo]}
                            /* O rastro do motor (`detalhe`) saiu do rodapé do card e entrou AQUI,
                               íntegro: ele é a prova de como o número saiu, não a leitura do
                               aluno. Ver `secaoDoMotor` em domain/metricas-explicacao.ts. */
                            secoes={[
                              ...METRICA_EXPLICACAO[tipo].secoes,
                              ...secaoDoMotor(estado === 'pronta' ? m?.detalhe : null),
                            ]}
                            ariaLabel={`O que é ${meta.nome.toLowerCase()}?`}
                          />
                        }
                      />
                    )
                  })}

                  <TileAutonomia value={resolucao.indiceAutonomiaIA} />
                </div>

                <NotaDeMetodo
                  estado={estado}
                  linguagem={resolucao.linguagem}
                  erro={metricasQuery.isError ? apiErrorMessage(metricasQuery.error) : null}
                  onRetry={() => void metricasQuery.refetch()}
                />
              </Card>

              {/* ------------------------------------ código + metadados */}
              <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1.62fr_1fr] lg:items-start">
                <CodeBlock
                  code={resolucao.codigoFonte}
                  lang={LINGUAGEM_META[resolucao.linguagem].codeLang}
                  label={`Solution.${EXTENSAO[resolucao.linguagem]}`}
                  maxHeight={420}
                />

                <div className="flex flex-col gap-[13px]">
                  {resolucao.descricaoApoioIA && (
                    <Card className="flex flex-col gap-2 px-[15px] py-3.5">
                      <span className="flex items-center gap-[7px] font-mono text-[11px] uppercase tracking-[.06em] text-mid">
                        <Gauge size={14} strokeWidth={2} aria-hidden className="text-steel" />
                        Como a IA ajudou
                      </span>
                      <p className="text-[12.5px] leading-[1.55] text-body">
                        {resolucao.descricaoApoioIA}
                      </p>
                    </Card>
                  )}

                  <div className="flex flex-col gap-2.5 rounded-ci border border-line bg-recess px-[15px] py-3.5">
                    <LinhaMeta chave="visibilidade">
                      <StatusChip status={ehPublica ? 'publico' : 'privado'} />
                    </LinhaMeta>
                    <LinhaMeta chave="linguagem">
                      <span className="flex items-center gap-[7px] font-mono text-[11px] text-body">
                        <LanguageDot lang={resolucao.linguagem} />
                        {LINGUAGEM_META[resolucao.linguagem].label}
                      </span>
                    </LinhaMeta>
                    <LinhaMeta chave="enviada em">
                      <span className="font-mono text-[11px] tabular-nums text-body">
                        {formatDate(resolucao.submetidaEm)}
                      </span>
                    </LinhaMeta>

                    <Button
                      variant="secondary"
                      size="sm"
                      fullWidth
                      icon={ehPublica ? Lock : Globe}
                      className="mt-1"
                      onClick={() => setVisibilidadeOpen(true)}
                    >
                      {ehPublica ? 'Tornar privada' : 'Publicar resolução'}
                    </Button>
                    <p className="text-[11.5px] leading-[1.5] text-soft">
                      {ehPublica
                        ? 'Visível a visitantes no portfólio público (quando o desafio é público).'
                        : 'Privada: só você vê. Publique para exibi-la no portfólio.'}
                    </p>
                  </div>

                  {/* Nota de imutabilidade (§4.1 do contrato: não existe editar) */}
                  <div className="flex gap-[9px] rounded-ci border border-atencao-line bg-atencao-bg px-[13px] py-[11px]">
                    <AlertTriangle
                      size={15}
                      strokeWidth={2}
                      aria-hidden
                      className="mt-px shrink-0 text-atencao"
                    />
                    <p className="text-[11.5px] leading-[1.5] text-mid">
                      Resoluções não são editadas. Para melhorar esta solução,{' '}
                      <Link
                        to={submeterHref}
                        className="font-medium text-steel underline-offset-2 hover:text-steel-hover hover:underline"
                      >
                        submeta uma nova resolução
                      </Link>{' '}
                      ao mesmo desafio — o histórico das tentativas é o dado da pesquisa.
                    </p>
                  </div>
                </div>
              </div>

              {/* Diálogo: alterar visibilidade */}
              <ConfirmDialog
                open={visibilidadeOpen}
                onOpenChange={setVisibilidadeOpen}
                icon={ehPublica ? Lock : Globe}
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
                title="Remover resolução?"
                description="Esta ação é permanente: a resolução e suas métricas serão apagadas. O ponto de dado desta tentativa desaparece da sua trajetória."
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

// --------------------------------------------------------------- métricas

/** Ciclomática é contagem (`M = 4`); tempo/espaço são classes (`O(n²)`). */
function valorDoTile(tipo: TipoMetrica, m: ResultadoMetricaDTO): string {
  return tipo === 'COMPLEXIDADE_CICLOMATICA' ? `M = ${m.valor}` : prettyBigO(m.rotulo)
}

/**
 * Rodapé do tile. Em `pronta`, o que o número QUER DIZER (`notaDaMetrica`) — e, nas métricas
 * Big-O, a confiança do próprio motor no valor que estimou (eixo distinto do selo ≈ ESTIMADO).
 * Sem métrica → o rótulo `sem métrica` (regra 7).
 *
 * ⚠ O `detalhe` do motor NÃO mora mais aqui (decisão desta rodada). Ele era o rastro auditável da
 * análise — "2 ponto(s) de decisao em 1 metodo(s)/construtor(es) (M = decisoes + P)" — impresso no
 * lugar da leitura do aluno. Continua íntegro, e agora dentro do `?` do próprio tile
 * (`secaoDoMotor`): nada sumiu, só foi para onde é lido de propósito.
 */
function notaDoTile(
  estado: EstadoFaixa,
  ehClasseBigO: boolean,
  tipo: TipoMetrica,
  m: ResultadoMetricaDTO | undefined,
) {
  if (estado === 'sem-metrica' || estado === 'vazio') return ROTULO_SEM_METRICA
  if (estado !== 'pronta' || !m) return undefined

  return (
    <>
      {notaDaMetrica(tipo, m)}
      {ehClasseBigO && (
        <span className="mt-1 block text-mid">
          confiança do motor: {NIVEL_CONFIANCA_LABEL[m.confianca]}
        </span>
      )}
    </>
  )
}

/** Linha de estado da faixa (direita do cabeçalho) — mono 10.5, `aria-live`. */
function LinhaDeEstado({
  estado,
  linguagem,
  analisadoEm,
}: {
  estado: EstadoFaixa
  linguagem: LinguagemProgramacao
  analisadoEm?: string
}) {
  const base = 'flex items-center gap-2 font-mono text-[10.5px] text-soft'

  if (estado === 'calculando') {
    return (
      <span role="status" aria-live="polite" className={base}>
        <RefreshCw size={12} strokeWidth={2} aria-hidden className="ci-spin text-mid" />
        análise em andamento · método: AST estática
      </span>
    )
  }

  if (estado === 'erro') {
    return (
      <span role="status" aria-live="polite" className={base}>
        <AlertTriangle size={12} strokeWidth={2} aria-hidden className="text-erro-texto" />
        falha ao carregar as métricas
      </span>
    )
  }

  if (estado === 'sem-metrica') {
    return (
      <span role="status" aria-live="polite" className={base}>
        <Cpu size={12} strokeWidth={2} aria-hidden className="text-atencao" />
        análise não aplicável · {LINGUAGEM_META[linguagem].label}
      </span>
    )
  }

  if (estado === 'vazio') {
    return (
      <span role="status" aria-live="polite" className={base}>
        <Cpu size={12} strokeWidth={2} aria-hidden className="text-atencao" />
        análise concluída · nenhuma métrica extraída
      </span>
    )
  }

  return (
    <span role="status" aria-live="polite" className={base}>
      <Check size={12} strokeWidth={2} aria-hidden className="text-sucesso" />
      <span className="tabular-nums">
        análise concluída · método: AST estática
        {analisadoEm ? ` · t = ${formatDate(analisadoEm)}` : ''}
      </span>
    </span>
  )
}

/** Tile 4: autonomia autodeclarada — neutra, nunca colormap (regra 4). */
function TileAutonomia({ value }: { value: number }) {
  return (
    <div className="flex flex-col justify-center gap-2.5 rounded-ci border border-line bg-recess px-[15px] py-3.5">
      <span className="font-mono text-[10.5px] uppercase tracking-[.06em] text-mid">
        Autonomia IA
      </span>
      <AutonomyMeter value={value} size="md" />
      <span className="font-mono text-[9.5px] text-soft">autodeclarada</span>
    </div>
  )
}

/** Rodapé da faixa: o contrato científico (MEDIDO × ≈ ESTIMADO) ou o motivo da ausência. */
function NotaDeMetodo({
  estado,
  linguagem,
  erro,
  onRetry,
}: {
  estado: EstadoFaixa
  linguagem: LinguagemProgramacao
  erro: string | null
  onRetry: () => void
}) {
  const caixa = 'flex gap-[9px] rounded-ci border border-line bg-recess px-[13px] py-[11px]'

  if (estado === 'erro') {
    return (
      <div
        role="alert"
        className="flex flex-wrap items-center gap-[9px] rounded-ci border border-erro-card-line bg-erro-card-bg px-[13px] py-[11px]"
      >
        <AlertTriangle
          size={15}
          strokeWidth={2}
          aria-hidden
          className="shrink-0 text-erro-texto"
        />
        <span className="flex-1 font-mono text-[10.5px] leading-[1.55] text-mid">
          {erro ?? 'Não foi possível carregar as métricas desta resolução.'}
        </span>
        <Button variant="ghost" size="sm" icon={RefreshCw} onClick={onRetry}>
          Tentar de novo
        </Button>
      </div>
    )
  }

  if (estado === 'sem-metrica') {
    return (
      <div className={caixa}>
        <Cpu size={15} strokeWidth={2} aria-hidden className="mt-px shrink-0 text-atencao" />
        <p className="font-mono text-[10.5px] leading-[1.55] text-soft">
          {NOTA_METRICAS_SO_JAVA} O motor de AST hoje não lê{' '}
          {LINGUAGEM_META[linguagem].label}. O índice de autonomia é autodeclarado e continua
          valendo.
        </p>
      </div>
    )
  }

  if (estado === 'vazio') {
    return (
      <div className={caixa}>
        <Cpu size={15} strokeWidth={2} aria-hidden className="mt-px shrink-0 text-atencao" />
        <p className="font-mono text-[10.5px] leading-[1.55] text-soft">
          A análise concluiu sem extrair métricas — o motor não conseguiu ler este código.
        </p>
      </div>
    )
  }

  return (
    <div className={caixa}>
      <BookOpen size={15} strokeWidth={2} aria-hidden className="mt-px shrink-0 text-steel" />
      {/* Mesma distinção de sempre (regra 3), dita sem jargão: "AST" e "análise estática" são o
          COMO, e o como já está no `?` de cada métrica. Aqui vale o que muda a leitura: um número
          foi contado, o outro foi deduzido. */}
      <p className="font-mono text-[10.5px] leading-[1.55] text-soft">
        <span className="text-ink">MEDIDO</span> = contado direto no seu código ·{' '}
        <span className="text-mid">≈ ESTIMADO</span> = deduzido da estrutura do código, sem
        executá-lo: é uma boa aproximação, não uma garantia.
      </p>
    </div>
  )
}

// --------------------------------------------------------------- auxiliares

/** Linha chave→valor do cartão de metadados. */
function LinhaMeta({ chave, children }: { chave: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <span className="font-mono text-[11px] text-soft">{chave}</span>
      {children}
    </div>
  )
}

/** Carregando a página: esqueletos `ciPulse` no cabeçalho, nos 4 tiles e no código. */
function EsqueletoResolucao() {
  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-[9px]">
        <Skeleton className="h-[26px] w-64" />
        <Skeleton className="h-[22px] w-72" />
      </div>

      <Card className="flex flex-col gap-3.5 px-[18px] py-4">
        <Skeleton className="h-[14px] w-40" />
        <div className="grid grid-cols-1 gap-[13px] sm:grid-cols-2 xl:grid-cols-[repeat(3,1fr)_0.92fr]">
          {[0, 1, 2, 3].map((i) => (
            <div
              key={i}
              className="flex flex-col gap-2.5 rounded-ci border border-line bg-recess px-[15px] py-3.5"
            >
              <Skeleton className="h-[11px] w-20" />
              <Skeleton className="h-8 w-24" />
              <Skeleton className="h-1.5 w-full" />
              <Skeleton className="h-[11px] w-3/5" />
            </div>
          ))}
        </div>
        <Skeleton className="h-10 w-full" />
      </Card>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1.62fr_1fr] lg:items-start">
        <Skeleton className="h-[420px] w-full" />
        <div className="flex flex-col gap-[13px]">
          <Skeleton className="h-28 w-full" />
          <Skeleton className="h-40 w-full" />
        </div>
      </div>
    </div>
  )
}
