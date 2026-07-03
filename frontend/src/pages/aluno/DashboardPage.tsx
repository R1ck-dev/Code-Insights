import { Link } from 'react-router-dom'
import type { LucideIcon } from 'lucide-react'
import {
  BarChart3,
  Braces,
  ChevronRight,
  Gauge,
  LineChart,
  Plus,
  Target,
  TrendingUp,
} from 'lucide-react'
import type { UseQueryResult } from '@tanstack/react-query'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { QueryBoundary } from '@/components/page/states'
import { Card } from '@/components/ui/card'
import { Chip } from '@/components/ui/badge'
import { buttonClasses } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { EmptyState } from '@/components/ui/empty-state'
import { VisibilityBadge } from '@/components/domain/badges'
import { useAuth } from '@/auth/useAuth'
import { useMeusDesafios } from '@/features/desafios/hooks'
import { useMeusSnippets } from '@/features/snippets/hooks'
import { formatDate } from '@/lib/utils'
import type { DesafioResumoDTO, Pagina } from '@/types/api'

/** Card de resumo com um número agregado (totalItens de uma listagem paginada). */
function CountCard<T>({
  icon: Icon,
  label,
  sub,
  query,
}: {
  icon: LucideIcon
  label: string
  sub?: string
  query: UseQueryResult<Pagina<T>>
}) {
  return (
    <Card className="flex flex-col gap-2.5 p-[18px]">
      <div className="flex items-center justify-between">
        <span className="text-[13px] font-semibold text-muted">{label}</span>
        <div className="flex h-8 w-8 items-center justify-center rounded-[9px] bg-brand/10">
          <Icon size={17} className="text-brand-strong" />
        </div>
      </div>
      <QueryBoundary query={query} loading={<Skeleton className="h-[34px] w-16" />}>
        {(pagina) => (
          <span className="font-mono text-[34px] font-bold leading-none text-heading tabular-nums">
            {pagina.totalItens}
          </span>
        )}
      </QueryBoundary>
      {sub && <span className="text-[12px] text-subtle">{sub}</span>}
    </Card>
  )
}

/** Chip sóbrio de painel de analytics ainda não disponível. */
function FutureChip({ icon: Icon, label }: { icon: LucideIcon; label: string }) {
  return (
    <span className="inline-flex items-center gap-2 rounded-lg border border-dashed border-border px-2.5 py-1.5 text-[12px] text-label">
      <Icon size={14} className="text-subtle" />
      {label}
      <span className="rounded bg-surface-2 px-1.5 py-0.5 text-[10.5px] font-medium uppercase tracking-wide text-subtle">
        em breve
      </span>
    </span>
  )
}

/** Linha de um desafio na lista "Desafios recentes". */
function RecentRow({ desafio }: { desafio: DesafioResumoDTO }) {
  return (
    <Link
      to={`/app/desafios/${desafio.id}`}
      className="group flex items-center gap-3 px-4 py-3 transition-colors hover:bg-surface-2"
    >
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-brand/[.08]">
        <Target size={15} className="text-brand-strong" />
      </div>
      <span className="flex-1 truncate text-[14px] font-medium text-fg">{desafio.titulo}</span>
      <VisibilityBadge visibilidade={desafio.visibilidade} />
      {desafio.plataformaOrigem && (
        <Chip className="hidden sm:inline-flex">{desafio.plataformaOrigem}</Chip>
      )}
      <span className="hidden w-[76px] shrink-0 text-right font-mono text-[11.5px] text-subtle tabular-nums sm:inline">
        {formatDate(desafio.criadoEm)}
      </span>
      <ChevronRight
        size={16}
        className="shrink-0 text-subtle transition-colors group-hover:text-fg"
      />
    </Link>
  )
}

export function DashboardPage() {
  const { user } = useAuth()
  const desafiosQuery = useMeusDesafios(0)
  const snippetsQuery = useMeusSnippets(0)

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

      {/* Cards de resumo — apenas totais que a API realmente expõe. */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <CountCard
          icon={Target}
          label="Desafios"
          sub="registrados no seu portfólio"
          query={desafiosQuery}
        />
        <CountCard
          icon={Braces}
          label="Snippets"
          sub="trechos reutilizáveis"
          query={snippetsQuery}
        />
        {/* Sem endpoint de agregados de análise: card remete às métricas por resolução. */}
        <Card className="flex flex-col gap-2.5 p-[18px]">
          <div className="flex items-center justify-between">
            <span className="text-[13px] font-semibold text-muted">Análises</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-[9px] bg-brand/10">
              <Gauge size={17} className="text-brand-strong" />
            </div>
          </div>
          <span className="text-[15px] font-semibold leading-tight text-heading">Por resolução</span>
          <span className="text-[12px] leading-relaxed text-subtle">
            Complexidade (Big O, ciclomática, espaço) e Índice de Autonomia IA em cada resolução
            analisada.
          </span>
        </Card>
      </div>

      {/* Painel design-forward: dashboards agregados chegam com o endpoint de analytics. */}
      <Card className="flex flex-col gap-4 p-5 md:flex-row md:items-start md:gap-5">
        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-brand/[.13]">
          <Gauge size={22} className="text-brand-strong" />
        </div>
        <div className="flex flex-1 flex-col gap-3">
          <div className="flex flex-col gap-1">
            <h3 className="text-[15px] font-semibold text-heading">Painéis de evolução a caminho</h3>
            <p className="max-w-2xl text-[13px] leading-relaxed text-muted">
              As análises por resolução já são calculadas hoje. Os painéis agregados — autonomia
              média × complexidade típica, evolução ao longo dos meses e distribuição de complexidade
              — chegam junto com o endpoint de analytics.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <FutureChip icon={LineChart} label="Autonomia × complexidade" />
            <FutureChip icon={TrendingUp} label="Evolução no tempo" />
            <FutureChip icon={BarChart3} label="Distribuição de complexidade" />
          </div>
        </div>
      </Card>

      {/* Desafios recentes */}
      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h3 className="text-[15px] font-semibold text-heading">Desafios recentes</h3>
          <Link
            to="/app/desafios"
            className="text-[13px] font-medium text-brand-strong hover:underline"
          >
            Ver todos
          </Link>
        </div>
        <QueryBoundary query={desafiosQuery}>
          {(pagina) =>
            pagina.itens.length === 0 ? (
              <EmptyState
                icon={Target}
                title="Comece registrando seu primeiro desafio"
                description="Cadastre um exercício de Online Judge e submeta suas resoluções para acompanhar suas métricas de aprendizado."
                action={
                  <Link to="/app/desafios" className={buttonClasses({ variant: 'primary', size: 'sm' })}>
                    <Plus size={15} />
                    Novo desafio
                  </Link>
                }
              />
            ) : (
              <Card className="flex flex-col divide-y divide-border-subtle overflow-hidden p-0">
                {pagina.itens.slice(0, 5).map((desafio) => (
                  <RecentRow key={desafio.id} desafio={desafio} />
                ))}
              </Card>
            )
          }
        </QueryBoundary>
      </section>
    </PageContainer>
  )
}
