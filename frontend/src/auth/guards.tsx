import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'
import { FullScreenLoader } from '@/components/ui/spinner'
import type { Role } from '@/types/api'

/** Protege rotas autenticadas: manda para /entrar guardando a origem. */
export function RequireAuth() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <FullScreenLoader />
  if (status === 'unauthenticated') {
    return <Navigate to="/entrar" replace state={{ from: location }} />
  }
  return <Outlet />
}

/**
 * Protege uma área por PAPEL, não só por autenticação. Quem está logado mas não tem o papel volta
 * para `/app` — e não para `/entrar`, que sugeriria que basta trocar de conta.
 *
 * O papel vem de `GET /api/usuarios/me`, mas quem decide de verdade é o `SecurityConfig`: esta
 * guarda só evita mostrar uma tela que a API recusaria. Esconder rota não é autorização.
 */
export function RequireRole({ roles }: { roles: readonly Role[] }) {
  const { status, user } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <FullScreenLoader />
  if (status === 'unauthenticated') {
    return <Navigate to="/entrar" replace state={{ from: location }} />
  }
  // Autenticado mas com o perfil ainda hidratando: esperar, não expulsar.
  if (!user) return <FullScreenLoader />
  if (!roles.includes(user.role)) return <Navigate to="/app" replace />

  return <Outlet />
}

/** Para páginas de acesso: se já está logado, vai para o app. */
export function RedirectIfAuthenticated() {
  const { status } = useAuth()

  if (status === 'loading') return <FullScreenLoader />
  if (status === 'authenticated') return <Navigate to="/app" replace />
  return <Outlet />
}
