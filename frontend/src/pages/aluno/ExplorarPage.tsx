import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Compass, Search, Users } from 'lucide-react'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { Input } from '@/components/ui/input'
import { Pagination } from '@/components/ui/pagination'
import { EmptyState } from '@/components/ui/empty-state'
import { Skeleton } from '@/components/ui/skeleton'
import { Avatar } from '@/components/Avatar'
import { useUsuariosPublicos } from '@/features/identity/hooks'

/**
 * Explorar: diretório de portfólios públicos de outros usuários.
 * Rota: /app/explorar (autenticada). Busca por username (com debounce) + paginação;
 * cada card leva ao portfólio público em /u/:id.
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
  const dados = query.data

  return (
    <PageContainer>
      <PageHeader
        title="Explorar"
        subtitle="Descubra portfólios públicos de outros usuários da plataforma."
      />

      <div className="relative">
        <Search
          size={16}
          className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-subtle"
        />
        <Input
          value={termo}
          onChange={(e) => setTermo(e.target.value)}
          placeholder="Buscar por nome de usuário…"
          className="pl-10"
          aria-label="Buscar usuários"
        />
      </div>

      {query.isPending ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[0, 1, 2, 3, 4, 5].map((k) => (
            <Skeleton key={k} className="h-[76px] w-full rounded-xl" />
          ))}
        </div>
      ) : query.isError ? (
        <EmptyState
          icon={Users}
          title="Não foi possível carregar"
          description="Tente novamente em instantes."
        />
      ) : dados && dados.itens.length === 0 ? (
        <EmptyState
          icon={Compass}
          title={filtro ? 'Nenhum usuário encontrado' : 'Nenhum portfólio público ainda'}
          description={
            filtro
              ? `Nada corresponde a "${filtro}".`
              : 'Quando outros usuários tornarem o portfólio público, eles aparecerão aqui.'
          }
        />
      ) : dados ? (
        <div className="flex flex-col gap-6">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {dados.itens.map((u) => (
              <Link
                key={u.id}
                to={`/u/${u.id}`}
                className="group flex items-center gap-3.5 rounded-xl border border-border bg-surface p-4 transition-colors hover:border-border-strong hover:bg-surface-2"
              >
                <Avatar name={u.username} size={44} />
                <div className="flex min-w-0 flex-col">
                  <span className="truncate text-[14.5px] font-semibold text-heading">
                    @{u.username}
                  </span>
                  <span className="text-[12px] text-subtle">Ver portfólio</span>
                </div>
              </Link>
            ))}
          </div>
          <Pagination page={pagina} totalPages={dados.totalPaginas} onChange={setPagina} />
        </div>
      ) : null}
    </PageContainer>
  )
}
