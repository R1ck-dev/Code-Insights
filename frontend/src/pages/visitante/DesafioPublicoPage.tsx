import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Code2, ExternalLink, Eye } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Chip } from '@/components/ui/badge'
import { buttonClasses } from '@/components/ui/button'
import { EmptyState } from '@/components/ui/empty-state'
import { Pagination } from '@/components/ui/pagination'
import { QueryBoundary, LoadingSection, ErrorState } from '@/components/page/states'
import { Avatar } from '@/components/Avatar'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import {
  AnalysisStatus,
  LanguageBadge,
  VisibilityBadge,
} from '@/components/domain/badges'
import { useDesafioDetalhe } from '@/features/desafios/hooks'
import { useResolucoesDoDesafio } from '@/features/resolucoes/hooks'
import { formatDate, pluralPt } from '@/lib/utils'
import { apiErrorMessage, apiErrorStatus } from '@/lib/api'

/** Visitante-03 — desafio público em modo leitura (sem ações de dono). */
export function DesafioPublicoPage() {
  const { usuarioId, desafioId } = useParams<{ usuarioId: string; desafioId: string }>()
  const desafioQuery = useDesafioDetalhe(desafioId)
  const [pagina, setPagina] = useState(0)
  const resolucoesQuery = useResolucoesDoDesafio(desafioId, pagina)

  const portfolioTo = `/u/${usuarioId ?? ''}`

  if (desafioQuery.isPending) {
    return (
      <div className="mx-auto w-full max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
        <LoadingSection />
      </div>
    )
  }

  // Lacuna da API: abrir o desafio ainda exige autenticação (401). Diferenciamos o motivo:
  // 401 → convida a entrar; 400/404 (não encontrado / sem acesso) → mostra a mensagem do backend.
  if (desafioQuery.isError || !desafioQuery.data) {
    const precisaLogin = apiErrorStatus(desafioQuery.error) === 401
    const msg = precisaLogin
      ? 'Entre para ver este desafio.'
      : apiErrorMessage(desafioQuery.error, 'Não foi possível carregar este desafio.')
    return (
      <div className="mx-auto flex w-full max-w-lg flex-col items-center gap-5 px-4 py-16 sm:px-6 lg:px-8">
        <ErrorState message={msg} className="w-full" />
        <div className="flex items-center gap-3">
          {precisaLogin ? (
            <>
              <Link to="/entrar" className={buttonClasses({ variant: 'primary' })}>
                Entrar
              </Link>
              <Link to="/criar-conta" className={buttonClasses({ variant: 'secondary' })}>
                Criar conta
              </Link>
            </>
          ) : (
            <Link to={portfolioTo} className={buttonClasses({ variant: 'secondary' })}>
              Voltar ao portfólio
            </Link>
          )}
        </div>
      </div>
    )
  }

  const desafio = desafioQuery.data

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-6 px-4 py-8 sm:px-6 lg:px-8">
      {/* Voltar ao portfólio do autor */}
      <Link
        to={portfolioTo}
        className="inline-flex w-fit items-center gap-2 text-[13px] font-medium text-brand-strong transition-colors hover:text-brand"
      >
        <ArrowLeft size={15} />
        Portfólio de @{desafio.autorUsername}
      </Link>

      {/* Cabeçalho: título, chips, visibilidade, datas, link externo */}
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-3">
          <h2 className="text-2xl font-bold tracking-tight text-heading">{desafio.titulo}</h2>
          <div className="flex flex-wrap items-center gap-2">
            {desafio.plataformaOrigem && <Chip>{desafio.plataformaOrigem}</Chip>}
            {desafio.identificadorExterno && <Chip mono>#{desafio.identificadorExterno}</Chip>}
            <VisibilityBadge visibilidade={desafio.visibilidade} />
            <span className="font-mono text-xs text-subtle">
              criado em {formatDate(desafio.criadoEm)}
            </span>
            {desafio.atualizadoEm !== desafio.criadoEm && (
              <span className="font-mono text-xs text-subtle">
                · atualizado em {formatDate(desafio.atualizadoEm)}
              </span>
            )}
          </div>
        </div>
        {desafio.urlExterna && (
          <a
            href={desafio.urlExterna}
            target="_blank"
            rel="noreferrer"
            className={buttonClasses({ variant: 'secondary' })}
          >
            Abrir link
            <ExternalLink size={15} className="text-muted" />
          </a>
        )}
      </div>

      {/* Enunciado + coluna lateral (autor / modo leitura) */}
      <div className="grid grid-cols-1 items-start gap-5 lg:grid-cols-[1.5fr_1fr]">
        <Card className="flex flex-col gap-3.5 p-5">
          <span className="text-sm font-semibold text-heading">Enunciado</span>
          {desafio.enunciado ? (
            <p className="whitespace-pre-wrap break-words text-sm leading-relaxed text-fg">
              {desafio.enunciado}
            </p>
          ) : (
            <p className="text-sm text-muted">Sem enunciado.</p>
          )}
        </Card>

        <div className="flex flex-col gap-4">
          <Card className="flex flex-col gap-4 p-[18px]">
            <span className="text-[13px] font-semibold text-muted">Sobre o autor</span>
            <div className="flex items-center gap-3">
              <Avatar name={desafio.autorUsername} size={44} />
              <span className="text-sm font-semibold text-heading">@{desafio.autorUsername}</span>
            </div>
            <Link
              to={portfolioTo}
              className={buttonClasses({ variant: 'secondary', size: 'sm', className: 'w-full' })}
            >
              Ver portfólio
            </Link>
          </Card>

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

      {/* Resoluções do desafio (leitura, sem ações de edição).
          Rótulo neutro: o endpoint não filtra por visibilidade da resolução. */}
      <section className="flex flex-col gap-4">
        <div className="flex items-center gap-2.5">
          <h3 className="text-base font-semibold text-heading">Resoluções</h3>
          <Chip mono>{pluralPt(desafio.qtdResolucoes, 'resolução', 'resoluções')}</Chip>
        </div>

        <QueryBoundary query={resolucoesQuery}>
          {(dados) =>
            dados.itens.length === 0 ? (
              <EmptyState
                icon={Code2}
                title="Nenhuma resolução"
                description="Este desafio ainda não tem resoluções."
              />
            ) : (
              <div className="flex flex-col gap-4">
                <ul className="flex flex-col gap-3">
                  {dados.itens.map((r) => (
                    <li key={r.id}>
                      <Card className="flex flex-wrap items-center justify-between gap-4 p-4">
                        <div className="flex flex-wrap items-center gap-3">
                          <LanguageBadge linguagem={r.linguagem} />
                          <AnalysisStatus analisada={r.analisada} />
                          <span className="font-mono text-[11.5px] text-subtle">
                            {formatDate(r.submetidaEm)}
                          </span>
                        </div>
                        <div className="flex items-center gap-2.5">
                          <span className="text-xs text-muted">Autonomia</span>
                          <AutonomyMeter size="sm" value={r.indiceAutonomiaIA} />
                        </div>
                      </Card>
                    </li>
                  ))}
                </ul>
                <Pagination
                  page={pagina}
                  totalPages={dados.totalPaginas}
                  onChange={setPagina}
                />
              </div>
            )
          }
        </QueryBoundary>
      </section>
    </div>
  )
}
