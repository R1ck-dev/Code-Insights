import { createContext, useCallback, useEffect, useMemo, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { identityApi } from '@/features/identity/api'
import { apiErrorStatus, tokenStore, UNAUTHORIZED_EVENT } from '@/lib/api'
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
  const queryClient = useQueryClient()

  const loadProfile = useCallback(async () => {
    try {
      const profile = await identityApi.buscarMeuPerfil()
      setUserState(profile)
      setStatus('authenticated')
    } catch (e) {
      // Só descarta o token quando o backend rejeita a credencial (401). Erros
      // transitórios (rede/5xx) NÃO deslogam: preserva o token para uma nova
      // tentativa em refresh/navegação — evita logout indevido por blip do servidor.
      if (apiErrorStatus(e) === 401) {
        tokenStore.clear()
      }
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

  // Se o backend rejeitar o token (401), desloga e descarta o cache do usuario.
  useEffect(() => {
    const onUnauthorized = () => {
      setUserState(null)
      setStatus('unauthenticated')
      queryClient.clear()
    }
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
  }, [queryClient])

  const login = useCallback(
    async (email: string, password: string) => {
      const { token } = await identityApi.login({ email, password })
      // Zera o cache do usuario anterior ANTES de carregar o novo perfil: as queries
      // de desafios/snippets nao sao chaveadas por usuario, entao sem isso os dados de
      // uma conta vazariam para a outra ao trocar de sessao (staleTime = 30s).
      queryClient.clear()
      tokenStore.set(token)
      await loadProfile()
    },
    [loadProfile, queryClient],
  )

  const logout = useCallback(() => {
    tokenStore.clear()
    setUserState(null)
    setStatus('unauthenticated')
    queryClient.clear()
  }, [queryClient])

  const setUser = useCallback((next: MeuPerfilDTO) => setUserState(next), [])

  const value = useMemo(
    () => ({ user, status, login, logout, refresh: loadProfile, setUser }),
    [user, status, login, logout, loadProfile, setUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
