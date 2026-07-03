import { api } from '@/lib/api'
import type {
  AlterarVisibilidadePerfilRequest,
  AtualizarMeuPerfilRequest,
  LoginRequest,
  MeuPerfilDTO,
  RegistrarUsuarioRequest,
  TokenResponse,
  UsuarioPublicoDTO,
} from '@/types/api'

export const identityApi = {
  registrar: (body: RegistrarUsuarioRequest) =>
    api.post<void>('/api/usuarios', body).then((r) => r.data),

  login: (body: LoginRequest) =>
    api.post<TokenResponse>('/api/auth/login', body).then((r) => r.data),

  ativarConta: (token: string) =>
    api
      .get<{ mensagem: string }>('/api/usuarios/ativar', { params: { token } })
      .then((r) => r.data),

  buscarMeuPerfil: () => api.get<MeuPerfilDTO>('/api/usuarios/me').then((r) => r.data),

  atualizarMeuPerfil: (body: AtualizarMeuPerfilRequest) =>
    api.patch<void>('/api/usuarios/me', body).then((r) => r.data),

  alterarVisibilidade: (body: AlterarVisibilidadePerfilRequest) =>
    api.patch<void>('/api/usuarios/me/visibilidade', body).then((r) => r.data),

  buscarUsuarioPublico: (usuarioId: string) =>
    api.get<UsuarioPublicoDTO>(`/api/usuarios/${usuarioId}`).then((r) => r.data),
}
