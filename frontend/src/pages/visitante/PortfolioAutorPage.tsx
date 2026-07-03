import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ChevronRight, FolderGit2, Lock } from 'lucide-react'
import { useUsuarioPublico } from '@/features/identity/hooks'
import { useDesafiosPublicosDoAutor } from '@/features/desafios/hooks'
import { QueryBoundary } from '@/components/page/states'
import { EmptyState } from '@/components/ui/empty-state'
import { Card } from '@/components/ui/card'
import { Chip } from '@/components/ui/badge'
import { Pagination } from '@/components/ui/pagination'
import { Avatar } from '@/components/Avatar'
import { VisibilityBadge } from '@/components/domain/badges'
import { formatDate } from '@/lib/utils'

/**
 * Portfólio público de um autor (visitante, sem login).
 * Rota: /u/:usuarioId — renderiza dentro do PublicLayout (via Outlet).
 * Perfil privado → portfólio indisponível. DesafioResumoDTO só expõe
 * titulo, plataformaOrigem, visibilidade e criadoEm (sem contagens).
 */
export function PortfolioAutorPage() {
  const { usuarioId } = useParams()
  const [pagina, setPagina] = useState(0)

  const usuarioQuery = useUsuarioPublico(usuarioId)
  const desafiosQuery = useDesafiosPublicosDoAutor(usuarioId ?? '', pagina)

  return (
    <div className="mx-auto w-full max-w-6xl px-5 py-10 sm:px-8">
      <QueryBoundary query={usuarioQuery}>
        {(usuario) =>
          usuario.visibilidadePerfil === 'PRIVADO' ? (
            <EmptyState
              icon={Lock}
              title="Portfólio indisponível"
              description="Este autor mantém o portfólio privado."
            />
          ) : (
            <div className="flex flex-col gap-8">
              {/* Cabeçalho do autor */}
              <Card className="flex flex-col gap-5 bg-brand/[.06] p-6 sm:flex-row sm:items-center sm:gap-6 sm:p-8">
                <Avatar name={usuario.username} size={72} />
                <div className="flex min-w-0 flex-1 flex-col gap-2">
                  <div className="flex flex-wrap items-center gap-3">
                    <h1 className="truncate text-2xl font-bold tracking-tight text-heading">
                      @{usuario.username}
                    </h1>
                    <VisibilityBadge visibilidade="PUBLICO" />
                  </div>
                </div>
                <div className="flex flex-col items-start gap-0.5 sm:items-end">
                  <span className="font-mono text-2xl font-bold tabular-nums text-heading">
                    {desafiosQuery.data?.totalItens ?? '—'}
                  </span>
                  <span className="text-xs text-subtle">desafios públicos</span>
                </div>
              </Card>

              {/* Lista de desafios públicos */}
              <section className="flex flex-col gap-4">
                <h2 className="text-[17px] font-semibold text-heading">Desafios públicos</h2>
                <QueryBoundary query={desafiosQuery}>
                  {(desafios) =>
                    desafios.itens.length === 0 ? (
                      <EmptyState
                        icon={FolderGit2}
                        title="Nenhum desafio público"
                        description="Este autor ainda não tornou nenhum desafio público."
                      />
                    ) : (
                      <div className="flex flex-col gap-6">
                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                          {desafios.itens.map((desafio) => (
                            <Link
                              key={desafio.id}
                              to={`/u/${usuarioId}/desafios/${desafio.id}`}
                              className="group flex flex-col gap-4 rounded-xl border border-border bg-surface p-5 transition-colors hover:border-border-strong hover:bg-surface-2"
                            >
                              <div className="flex items-start justify-between gap-2.5">
                                <span className="text-[15px] font-semibold leading-snug text-heading">
                                  {desafio.titulo}
                                </span>
                                {desafio.plataformaOrigem && (
                                  <Chip className="shrink-0">{desafio.plataformaOrigem}</Chip>
                                )}
                              </div>
                              <div className="mt-auto flex items-center justify-between border-t border-border-subtle pt-3">
                                <span className="font-mono text-[11.5px] tabular-nums text-subtle">
                                  {formatDate(desafio.criadoEm)}
                                </span>
                                <ChevronRight
                                  size={15}
                                  className="text-brand-strong transition-transform group-hover:translate-x-0.5"
                                />
                              </div>
                            </Link>
                          ))}
                        </div>
                        <Pagination
                          page={pagina}
                          totalPages={desafios.totalPaginas}
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
      </QueryBoundary>
    </div>
  )
}
