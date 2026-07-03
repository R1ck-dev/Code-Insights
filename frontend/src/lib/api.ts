import axios from 'axios'

const TOKEN_KEY = 'codeinsights.token'

/** Guarda do JWT no localStorage. */
export const tokenStore = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

/** Evento disparado quando o backend rejeita o token (401): a app desloga. */
export const UNAUTHORIZED_EVENT = 'codeinsights:unauthorized'

/**
 * Em dev, baseURL vazio → chamadas para "/api/..." caem no proxy do Vite (:8080).
 * Em produção, defina VITE_API_BASE_URL para a URL pública do backend.
 */
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
})

api.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      tokenStore.clear()
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
    }
    return Promise.reject(error)
  },
)

/**
 * Extrai uma mensagem legível do erro da API. O backend responde:
 *  - regra de negócio → 400 { "erro": "..." }
 *  - validação → 400 { "campo": "mensagem", ... }
 */
export function apiErrorMessage(
  error: unknown,
  fallback = 'Ocorreu um erro. Tente novamente.',
): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as unknown
    if (data && typeof data === 'object') {
      const record = data as Record<string, unknown>
      if (typeof record.erro === 'string') return record.erro
      const first = Object.values(record).find((v) => typeof v === 'string')
      if (typeof first === 'string') return first
    }
    if (error.code === 'ERR_NETWORK') {
      return 'Não foi possível conectar ao servidor. Ele está rodando?'
    }
  }
  return fallback
}

/** Status HTTP de um erro da API (undefined se não for erro HTTP). */
export function apiErrorStatus(error: unknown): number | undefined {
  return axios.isAxiosError(error) ? error.response?.status : undefined
}
