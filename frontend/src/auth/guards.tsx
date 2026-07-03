import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'
import { FullScreenLoader } from '@/components/ui/spinner'

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

/** Para páginas de acesso: se já está logado, vai para o app. */
export function RedirectIfAuthenticated() {
  const { status } = useAuth()

  if (status === 'loading') return <FullScreenLoader />
  if (status === 'authenticated') return <Navigate to="/app" replace />
  return <Outlet />
}
