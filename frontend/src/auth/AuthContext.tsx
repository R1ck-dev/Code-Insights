import { createContext, useCallback, useEffect, useMemo, useState } from 'react'
import { identityApi } from '@/features/identity/api'
import { tokenStore, UNAUTHORIZED_EVENT } from '@/lib/api'
import type { MeuPerfilDTO } from '@/types/api'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

interface AuthContextValue {
  user: MeuPerfilDTO | null
  status: AuthStatus
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  refresh: () => Promise<void>
  setUser: (user: MeuPerfilDTO) => void
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUserState] = useState<MeuPerfilDTO | null>(null)
  const [status, setStatus] = useState<AuthStatus>(() =>
    tokenStore.get() ? 'loading' : 'unauthenticated',
  )

  const loadProfile = useCallback(async () => {
    try {
      const profile = await identityApi.buscarMeuPerfil()
      setUserState(profile)
      setStatus('authenticated')
    } catch {
      tokenStore.clear()
      setUserState(null)
      setStatus('unauthenticated')
    }
  }, [])

  // Carrega o perfil no boot, se houver token guardado.
  useEffect(() => {
    if (tokenStore.get()) {
      void loadProfile()
    }
  }, [loadProfile])

  // Se o backend rejeitar o token (401), desloga.
  useEffect(() => {
    const onUnauthorized = () => {
      setUserState(null)
      setStatus('unauthenticated')
    }
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
  }, [])

  const login = useCallback(
    async (email: string, password: string) => {
      const { token } = await identityApi.login({ email, password })
      tokenStore.set(token)
      await loadProfile()
    },
    [loadProfile],
  )

  const logout = useCallback(() => {
    tokenStore.clear()
    setUserState(null)
    setStatus('unauthenticated')
  }, [])

  const setUser = useCallback((next: MeuPerfilDTO) => setUserState(next), [])

  const value = useMemo(
    () => ({ user, status, login, logout, refresh: loadProfile, setUser }),
    [user, status, login, logout, loadProfile, setUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
