import { api } from '@/lib/api'
import type {
  AtualizarDesafioRequest,
  CriarDesafioRequest,
  DesafioDetalheDTO,
  DesafioResumoDTO,
  Pagina,
} from '@/types/api'

export const desafiosApi = {
  listarMeus: (pagina: number, tamanho: number) =>
    api
      .get<Pagina<DesafioResumoDTO>>('/api/desafios', { params: { pagina, tamanho } })
      .then((r) => r.data),

  listarPublicosDoAutor: (autorId: string, pagina: number, tamanho: number) =>
    api
      .get<Pagina<DesafioResumoDTO>>(`/api/desafios/autor/${autorId}`, {
        params: { pagina, tamanho },
      })
      .then((r) => r.data),

  buscarDetalhe: (id: string) =>
    api.get<DesafioDetalheDTO>(`/api/desafios/${id}`).then((r) => r.data),

  criar: (body: CriarDesafioRequest) =>
    api.post<DesafioResumoDTO>('/api/desafios', body).then((r) => r.data),

  atualizar: (id: string, body: AtualizarDesafioRequest) =>
    api.patch<void>(`/api/desafios/${id}`, body).then((r) => r.data),

  alterarVisibilidade: (id: string, publico: boolean) =>
    api.patch<void>(`/api/desafios/${id}/visibilidade`, { publico }).then((r) => r.data),

  remover: (id: string) => api.delete<void>(`/api/desafios/${id}`).then((r) => r.data),
}
