import { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Book, Cpu, Eye, Gauge, Info } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { EmptyState } from '@/components/ui/empty-state'
import { buttonClasses } from '@/components/ui/button'
import { InfoButton } from '@/components/ui/info-button'
import { LoadingSection, ErrorState } from '@/components/page/states'
import { CodeBlock, type CodeLang } from '@/components/CodeBlock'
import { MetricTile } from '@/components/MetricTile'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { LangChip, LanguageDot, StatusChip } from '@/components/domain/badges'
import {
  LINGUAGEM_COM_METRICAS,
  LINGUAGEM_META,
  NOTA_METRICAS_SO_JAVA,
  prettyBigO,
  TIPO_METRICA_META,
} from '@/domain/enums'
import { METRICA_EXPLICACAO } from '@/domain/metricas-explicacao'
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

/** Extensão do arquivo mostrada no cabeçalho do CodeBlock (`Solution.java`). */
const EXTENSAO: Record<CodeLang, string> = {
  java: 'java',
  python: 'py',
  cpp: 'cpp',
  javascript: 'js',
  c: 'c',
}

/** Colunas dos tiles 1–3 quando o lugar deles é ocupado por um estado. */
const VAO_DOS_TILES = 'sm:col-span-2 lg:col-span-3'

type EstadoMetricas = 'calculando' | 'erro' | 'sem-java' | 'vazio' | 'ok'

/**
 * Visitante — resolução pública em MODO LEITURA (00-INDICE §6-A, Lacuna 4:
 * extrapolada da tela D em leitura). Mesma faixa "RETRATO DE MÉTRICAS" da tela D
 * (ciclomática MEDIDO · tempo/espaço ≈ ESTIMADO com barra de colormap), o mesmo
 * CodeBlock e o card "Como a IA ajudou" — SEM nenhuma ação de dono (nova resolução,
 * remover, alterar visibilidade) e COM o banner "modo leitura" da tela K.
 *
 * Rota: /u/:usuarioId/desafios/:desafioId/resolucoes/:resolucaoId (PublicLayout).
 * O backend só entrega resolução pública sob desafio público (senão 400/401).
 */
export function ResolucaoPublicaPage() {
  const { usuarioId, desafioId, resolucaoId } = useParams()
  const resolucaoQuery = useResolucaoDetalhe(resolucaoId)
  const resolucao = resolucaoQuery.data

  const desafioQuery = useDesafioDetalhe(desafioId)
  const tituloDesafio = desafioQuery.data?.titulo
  const autorUsername = desafioQuery.data?.autorUsername

  const metricasQuery = useMetricasDaResolucao(resolucaoId)
  const analisada = resolucao?.analisada
  useEffect(() => {
    if (analisada) void metricasQuery.refetch()
  }, [analisada, metricasQuery.refetch])

  const desafioTo = `/u/${usuarioId ?? ''}/desafios/${desafioId ?? ''}`

  if (resolucaoQuery.isPending) {
    return (
      <div className="mx-auto w-full max-w-[1320px] px-5 py-[26px] sm:px-8 lg:px-12">
        <LoadingSection label="carregando resolução" />
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
      <div className="mx-auto flex w-full max-w-[520px] flex-col items-center gap-5 px-5 py-16 sm:px-8">
        <ErrorState message={msg} className="w-full" />
        <div className="flex flex-wrap items-center justify-center gap-[9px]">
          {precisaLogin ? (
            <Link to="/entrar" className={buttonClasses({ size: 'sm' })}>
              Entrar
            </Link>
          ) : null}
          <Link to={desafioTo} className={buttonClasses({ variant: 'secondary', size: 'sm' })}>
            Voltar ao desafio
          </Link>
        </div>
      </div>
    )
  }

  const metricas: ResultadoMetricaDTO[] = metricasQuery.data ?? []
  const porTipo = new Map(metricas.map((m) => [m.tipo, m]))
  const semMetricas = metricas.length === 0
  const naoJava = resolucao.linguagem !== LINGUAGEM_COM_METRICAS
  const linguagem = LINGUAGEM_META[resolucao.linguagem]

  // Mesma precedência de sempre — só o vestuário mudou.
  const estado: EstadoMetricas = !resolucao.analisada
    ? 'calculando'
    : metricasQuery.isError
      ? 'erro'
      : semMetricas && metricasQuery.isFetching
        ? 'calculando'
        : semMetricas && naoJava
          ? 'sem-java'
          : semMetricas
            ? 'vazio'
            : 'ok'

  return (
    <div className="mx-auto flex w-full max-w-[1320px] flex-col gap-[22px] px-5 py-[26px] pb-10 sm:px-8 lg:px-12">
      <Link
        to={desafioTo}
        className="inline-flex w-fit items-center gap-2 font-mono text-[12.5px] text-steel transition-colors hover:text-steel-hover"
      >
        <ArrowLeft size={15} strokeWidth={2} aria-hidden />
        {tituloDesafio ? `Voltar para ${tituloDesafio}` : 'Voltar ao desafio'}
      </Link>

      {/* Cabeçalho da resolução (tela D §3.1, sem as ações de dono) */}
      <header className="flex min-w-0 flex-col gap-[9px]">
        <h1 className="text-[25px] font-semibold leading-tight tracking-[-.02em] text-ink">
          {tituloDesafio ?? 'Resolução'}
        </h1>
        <div className="flex flex-wrap items-center gap-[9px]">
          <LangChip lang={resolucao.linguagem} />
          <StatusChip status={resolucao.visibilidade === 'PUBLICO' ? 'publico' : 'privado'} />
          <span className="font-mono text-[11px] tabular-nums text-soft">
            enviada em {formatDateTime(resolucao.submetidaEm)}
          </span>
        </div>
      </header>

      {/* ★ Faixa de métricas — o retrato (tela D §3.2) */}
      <Card className="flex flex-col gap-3.5 p-[18px]">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="font-mono text-[11px] uppercase tracking-[.12em] text-mid">
            Retrato de métricas
          </h2>
          <div className="flex items-center gap-2">
            <StatusChip status={resolucao.analisada ? 'analisada' : 'calculando'} />
            <span className="font-mono text-[10.5px] text-soft">método: AST estática</span>
          </div>
        </div>

        <div className="grid gap-[13px] sm:grid-cols-2 lg:grid-cols-[repeat(3,minmax(0,1fr))_0.92fr]">
          {estado === 'erro' ? (
            <ErrorState
              className={VAO_DOS_TILES}
              title="Não foi possível ler as métricas"
              message={apiErrorMessage(metricasQuery.error)}
              onRetry={() => void metricasQuery.refetch()}
            />
          ) : estado === 'vazio' ? (
            <EmptyState
              className={VAO_DOS_TILES}
              size="sm"
              icon={Info}
              title="Sem métricas"
              description="A análise concluiu, mas não retornou métricas para esta resolução."
            />
          ) : (
            ORDEM_METRICAS.map((tipo) => {
              const meta = TIPO_METRICA_META[tipo]
              const m = porTipo.get(tipo)
              const calculando = estado === 'calculando'
              // `valor` cru, sem o `≈` — o MetricTile prefixa quando ESTIMADO.
              const valor =
                calculando || !m
                  ? null
                  : meta.ehClasseBigO
                    ? prettyBigO(m.rotulo)
                    : `M = ${m.rotulo}`
              return (
                <MetricTile
                  key={tipo}
                  rotulo={meta.rotulo}
                  metodo={meta.metodo}
                  valor={valor}
                  confianca={meta.confianca}
                  k={meta.ehClasseBigO && m ? m.valor : null}
                  calculando={calculando}
                  barra={meta.ehClasseBigO}
                  nota={!calculando && m?.detalhe ? m.detalhe : undefined}
                  info={
                    <InfoButton
                      {...METRICA_EXPLICACAO[tipo]}
                      ariaLabel={`O que é ${meta.nome.toLowerCase()}?`}
                    />
                  }
                />
              )
            })
          )}

          {/* Tile 4 — autonomia: neutra, autodeclarada, existe mesmo sem métrica de complexidade */}
          <div className="flex flex-col justify-center gap-2.5 rounded-ci border border-line bg-recess px-[15px] py-3.5">
            <span className="font-mono text-[10.5px] uppercase tracking-[.06em] text-mid">
              Autonomia IA
            </span>
            <AutonomyMeter value={resolucao.indiceAutonomiaIA} size="md" />
            <span className="font-mono text-[9.5px] text-soft">autodeclarada</span>
          </div>
        </div>

        {/* Nota de método (rodapé da faixa) */}
        {estado === 'sem-java' ? (
          <div className="flex items-start gap-[9px] rounded-ci border border-line bg-recess px-[13px] py-[11px]">
            <Cpu size={15} strokeWidth={2} aria-hidden className="mt-px shrink-0 text-atencao-ink" />
            <p className="font-mono text-[10.5px] leading-[1.55] text-soft">
              {NOTA_METRICAS_SO_JAVA} Esta resolução está em {linguagem.label} — o retrato mostra só a
              autonomia autodeclarada.
            </p>
          </div>
        ) : (
          <div className="flex items-start gap-[9px] rounded-ci border border-line bg-recess px-[13px] py-[11px]">
            <Book size={15} strokeWidth={2} aria-hidden className="mt-px shrink-0 text-steel" />
            {/* Vocabulário do sistema: "análise estática", não "heurística" (§5, divergência 8) —
                é a mesma frase da tela D, e as duas telas mostram o mesmo retrato. */}
            <p className="font-mono text-[10.5px] leading-[1.55] text-soft">
              <span className="text-ink">MEDIDO</span> = contagem direta no AST ·{' '}
              <span className="text-mid">≈ ESTIMADO</span> = inferido por análise estática, pode
              divergir do pior caso real.
            </p>
          </div>
        )}
      </Card>

      {/* Código + metadados (tela D §3.3, sem a nota de imutabilidade — ela é do dono) */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1.62fr_1fr] lg:items-start">
        <CodeBlock
          code={resolucao.codigoFonte}
          lang={linguagem.codeLang}
          label={`Solution.${EXTENSAO[linguagem.codeLang]}`}
          maxHeight={460}
        />

        <aside className="flex flex-col gap-[13px]">
          {resolucao.descricaoApoioIA && (
            <Card className="flex flex-col gap-2 px-[15px] py-3.5">
              <span className="flex items-center gap-[7px] font-mono text-[11px] uppercase tracking-[.06em] text-mid">
                <Gauge size={14} strokeWidth={2} aria-hidden className="text-steel" />
                Como a IA ajudou
              </span>
              <p className="text-[12.5px] leading-[1.55] text-body">{resolucao.descricaoApoioIA}</p>
            </Card>
          )}

          <div className="flex flex-col gap-2.5 rounded-ci border border-line bg-recess px-[15px] py-3.5">
            <div className="flex items-center justify-between gap-3">
              <span className="font-mono text-[11px] text-soft">visibilidade</span>
              <StatusChip status={resolucao.visibilidade === 'PUBLICO' ? 'publico' : 'privado'} />
            </div>
            <div className="flex items-center justify-between gap-3">
              <span className="font-mono text-[11px] text-soft">linguagem</span>
              <span className="flex items-center gap-[7px] font-mono text-[11px] text-body">
                <LanguageDot lang={resolucao.linguagem} />
                {linguagem.label}
              </span>
            </div>
            <div className="flex items-center justify-between gap-3">
              <span className="font-mono text-[11px] text-soft">enviada em</span>
              <span className="font-mono text-[11px] tabular-nums text-body">
                {formatDateTime(resolucao.submetidaEm)}
              </span>
            </div>
            {autorUsername && (
              <div className="flex items-center justify-between gap-3">
                <span className="font-mono text-[11px] text-soft">autor</span>
                <Link
                  to={`/u/${usuarioId ?? ''}`}
                  className="font-mono text-[11px] font-medium text-steel transition-colors hover:text-steel-hover"
                >
                  @{autorUsername}
                </Link>
              </div>
            )}
          </div>

          {/* Banner "modo leitura" (tela K §c2) */}
          <div className="flex items-start gap-[9px] rounded-ci border border-line bg-recess px-[14px] py-[13px]">
            <Eye size={15} strokeWidth={2} aria-hidden className="mt-px shrink-0 text-steel" />
            <p className="text-[12px] leading-[1.5] text-soft">
              Você está vendo esta resolução em modo leitura.{' '}
              <Link
                to="/criar-conta"
                className="font-semibold text-steel transition-colors hover:text-steel-hover"
              >
                Crie uma conta
              </Link>{' '}
              para montar o seu.
            </p>
          </div>
        </aside>
      </div>
    </div>
  )
}
