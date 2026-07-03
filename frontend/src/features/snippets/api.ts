import { api } from '@/lib/api'
import type {
  AtualizarSnippetRequest,
  CriarSnippetRequest,
  Pagina,
  SnippetDTO,
} from '@/types/api'

export const snippetsApi = {
  listarMeus: (pagina: number, tamanho: number) =>
    api
      .get<Pagina<SnippetDTO>>('/api/snippets', { params: { pagina, tamanho } })
      .then((r) => r.data),

  buscarDetalhe: (id: string) => api.get<SnippetDTO>(`/api/snippets/${id}`).then((r) => r.data),

  criar: (body: CriarSnippetRequest) =>
    api.post<SnippetDTO>('/api/snippets', body).then((r) => r.data),

  atualizar: (id: string, body: AtualizarSnippetRequest) =>
    api.patch<void>(`/api/snippets/${id}`, body).then((r) => r.data),

  remover: (id: string) => api.delete<void>(`/api/snippets/${id}`).then((r) => r.data),
}
