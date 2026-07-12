import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { FolderGit2, Globe, Lock } from 'lucide-react'
import { useUsuarioPublico } from '@/features/identity/hooks'
import { useDesafiosPublicosDoAutor } from '@/features/desafios/hooks'
import { QueryBoundary } from '@/components/page/states'
import { EmptyState } from '@/components/ui/empty-state'
import { Badge } from '@/components/ui/badge'
import { Pagination } from '@/components/ui/pagination'
import { Skeleton } from '@/components/ui/skeleton'
import { Avatar } from '@/components/Avatar'
import { Nebula } from '@/components/Nebula'
import { DesafioCard } from '@/components/domain/DesafioCard'
import { pluralPt } from '@/lib/utils'

/*
 * J · Portfólio público de um autor (visitante, sem login) — spec 03 §J.
 * Rota: /u/:usuarioId, dentro do PublicLayout (a nav pública é da moldura).
 *
 * Estrutura: header do autor (avatar 72px + @username + stats) sobre a nebulosa
 * `portfolio`, hairline embaixo; corpo com a grade de DesafioCard + paginação.
 *
 * Honestidade de dado: `UsuarioPublicoDTO` só traz `id/username/visibilidadePerfil`
 * e `DesafioResumoDTO` não traz identificador nem contagem de resoluções — então o
 * header mostra APENAS a contagem que existe (desafios públicos) e os cards não
 * inventam nº de resoluções. Nada de métrica nesta tela (o portfólio lista desafios).
 */

/** Padding lateral do plate público (48px na spec) e teto de 1320px. */
const PLATE = 'mx-auto w-full max-w-[1320px] px-5 sm:px-8 lg:px-12'

export function PortfolioAutorPage() {
  const { usuarioId } = useParams()
  const [pagina, setPagina] = useState(0)

  const usuarioQuery = useUsuarioPublico(usuarioId)
  const desafiosQuery = useDesafiosPublicosDoAutor(usuarioId ?? '', pagina)

  return (
    <QueryBoundary query={usuarioQuery}>
      {(usuario) =>
        usuario.visibilidadePerfil === 'PRIVADO' ? (
          <div className={`${PLATE} py-14`}>
            <EmptyState
              icon={Lock}
              title="Portfólio indisponível"
              description="Este autor mantém o portfólio privado."
            />
          </div>
        ) : (
          <>
            {/* Header do autor — nebulosa no canto superior esquerdo, sem starfield (spec 03 §J.2) */}
            <header className="relative overflow-hidden border-b border-line-soft">
              <Nebula variant="portfolio" />
              <div
                className={`${PLATE} relative flex flex-col gap-5 py-8 sm:flex-row sm:items-center sm:gap-[22px] sm:py-9`}
              >
                <Avatar
                  name={usuario.username}
                  size={72}
                  className="text-[26px]! font-bold"
                />

                <div className="flex min-w-0 flex-1 flex-col gap-[9px]">
                  <div className="flex flex-wrap items-center gap-3">
                    <h1 className="truncate text-[26px] leading-tight font-bold tracking-[-.02em] text-ink">
                      @{usuario.username}
                    </h1>
                    <Badge tom="sucesso" icon={Globe}>
                      Perfil público
                    </Badge>
                  </div>
                  <p className="text-[14px] text-mid">
                    Desafios e resoluções compartilhados publicamente.
                  </p>
                </div>

                <dl className="flex shrink-0 gap-7 sm:flex-col sm:items-end sm:gap-1">
                  <div className="flex flex-col gap-0.5 sm:items-end">
                    <dd className="tabular font-mono text-[26px] leading-none font-bold text-ink">
                      {desafiosQuery.data?.totalItens ?? '—'}
                    </dd>
                    <dt className="text-[12px] text-soft">desafios públicos</dt>
                  </div>
                </dl>
              </div>
            </header>

            {/* Corpo — grade de desafios públicos (spec 03 §J.3) */}
            <section className={`${PLATE} flex flex-col gap-[18px] pt-[26px] pb-10`}>
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-[17px] font-semibold text-ink">Desafios públicos</h2>
                {desafiosQuery.data && desafiosQuery.data.totalItens > 0 && (
                  <span className="tabular font-mono text-[12px] text-soft">
                    {pluralPt(desafiosQuery.data.totalItens, 'desafio', 'desafios')}
                  </span>
                )}
              </div>

              <QueryBoundary query={desafiosQuery} loading={<GradeEsqueleto />}>
                {(desafios) =>
                  desafios.itens.length === 0 ? (
                    <EmptyState
                      icon={FolderGit2}
                      title="Nenhum desafio público ainda."
                      description="Este autor ainda não tornou nenhum desafio público."
                    />
                  ) : (
                    <div className="flex flex-col gap-5">
                      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {desafios.itens.map((desafio) => (
                          <DesafioCard
                            key={desafio.id}
                            to={`/u/${usuarioId}/desafios/${desafio.id}`}
                            titulo={desafio.titulo}
                            plataforma={desafio.plataformaOrigem}
                            criadoEm={desafio.criadoEm}
                            chevron
                          />
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
          </>
        )
      }
    </QueryBoundary>
  )
}

/** 6 cards-esqueleto (ciPulse), na geometria exata do DesafioCard. */
function GradeEsqueleto() {
  return (
    <div
      role="status"
      aria-busy
      aria-label="Carregando desafios"
      className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3"
    >
      {Array.from({ length: 6 }, (_, i) => (
        <div
          key={i}
          className="flex flex-col gap-3 rounded-ci border border-line bg-panel p-[17px]"
        >
          <Skeleton className="h-[15px] w-4/5" />
          <Skeleton className="h-[19px] w-[92px]" />
          <div className="mt-auto flex items-center justify-between border-t border-line-soft pt-[11px]">
            <Skeleton className="h-[11px] w-[70px]" />
            <Skeleton className="h-[11px] w-[62px]" />
          </div>
        </div>
      ))}
    </div>
  )
}
