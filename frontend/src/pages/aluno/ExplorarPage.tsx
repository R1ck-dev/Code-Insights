import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { Folder, Search } from 'lucide-react'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { QueryBoundary } from '@/components/page/states'
import { AutorCard } from '@/components/domain/AutorCard'
import { Input } from '@/components/ui/input'
import { EmptyState } from '@/components/ui/empty-state'
import { Pagination } from '@/components/ui/pagination'
import { Skeleton } from '@/components/ui/skeleton'
import { useUsuariosPublicos } from '@/features/identity/hooks'
import { cn, pluralPt } from '@/lib/utils'

/**
 * Explorar — diretório de portfólios públicos (00-INDICE §4.2 + §6-A, Lacuna 1).
 *
 * O MESMO componente serve DUAS rotas:
 *   · `/explorar`      → público, dentro do PublicLayout (destino do CTA da landing)
 *   · `/app/explorar`  → autenticado, dentro do AppLayout (item `Explorar` da sidebar)
 * Por isso nada aqui pressupõe usuário logado — os portfólios listados já são públicos e
 * `GET /api/usuarios/publicos` é `permitAll`. A única diferença entre os dois contextos é a
 * largura: no shell público não há sidebar, então a página limita a própria medida.
 *
 * Tela de listagem: SEM starfield/nebulosa — só o `bg` liso.
 */
export function ExplorarPage() {
  const [termo, setTermo] = useState('')
  const [filtro, setFiltro] = useState('')
  const [pagina, setPagina] = useState(0)

  // Debounce da busca (300ms); qualquer novo filtro volta para a primeira página.
  useEffect(() => {
    const t = setTimeout(() => {
      setFiltro(termo.trim())
      setPagina(0)
    }, 300)
    return () => clearTimeout(t)
  }, [termo])

  const query = useUsuariosPublicos(filtro, pagina)
  const total = query.data?.totalItens

  // Rota pública (fora do shell): a página é dona da própria largura.
  const noApp = useLocation().pathname.startsWith('/app')

  return (
    <PageContainer className={cn(!noApp && 'mx-auto max-w-[1140px] py-8 md:py-10')}>
      <PageHeader
        title="Explorar"
        subtitle={
          total != null ? (
            <span className="tabular">{pluralPt(total, 'portfólio público', 'portfólios públicos')}</span>
          ) : undefined
        }
      />

      {/* Barra de busca — no shell autenticado ela vive na topbar variante (b); aqui fica no corpo
          para que a mesma tela funcione também na rota pública, que não tem topbar de busca. */}
      <div className="flex items-center gap-[9px]">
        <Input
          value={termo}
          onChange={(e) => setTermo(e.target.value)}
          type="search"
          mono
          icon={Search}
          placeholder="buscar pessoas…"
          aria-label="Buscar pessoas por nome de usuário"
          className="w-full max-w-[320px]"
        />
      </div>

      <QueryBoundary query={query} loading={<GradeEsqueleto />}>
        {(dados) =>
          dados.itens.length === 0 ? (
            filtro ? (
              <EmptyState
                icon={Search}
                title="Nenhum portfólio encontrado."
                description={`Nada corresponde a “${filtro}”.`}
              />
            ) : (
              <EmptyState
                icon={Folder}
                title="Nenhum portfólio público ainda."
                description="Quando alguém tornar o perfil público, o portfólio aparece aqui."
              />
            )
          ) : (
            <div className="flex flex-col gap-5">
              <ul
                aria-label="Portfólios públicos"
                aria-busy={query.isFetching || undefined}
                className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3"
              >
                {dados.itens.map((u) => (
                  <li key={u.id} className="min-w-0">
                    <AutorCard usuarioId={u.id} username={u.username} />
                  </li>
                ))}
              </ul>

              <Pagination page={pagina} totalPages={dados.totalPaginas} onChange={setPagina} />
            </div>
          )
        }
      </QueryBoundary>
    </PageContainer>
  )
}

/** Carregando: 6 cards-esqueleto com o mesmo esqueleto do AutorCard (§4.2). */
function GradeEsqueleto() {
  return (
    <div
      role="status"
      aria-label="Carregando portfólios"
      className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3"
    >
      {[0, 1, 2, 3, 4, 5].map((i) => (
        <div
          key={i}
          className="flex flex-col gap-[13px] rounded-ci border border-line bg-panel p-[17px]"
        >
          <div className="flex items-center gap-3">
            <Skeleton className="h-11 w-11 shrink-0" />
            <div className="flex min-w-0 flex-1 flex-col gap-2">
              <Skeleton className="h-[13px] w-1/2" />
              <Skeleton className="h-[10px] w-2/3" />
            </div>
          </div>
          <div className="border-t border-line-soft pt-[11px]">
            <Skeleton className="h-[10px] w-24" />
          </div>
        </div>
      ))}
    </div>
  )
}
