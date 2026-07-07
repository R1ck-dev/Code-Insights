import { api } from '@/lib/api'
import type {
  AtualizarSnippetRequest,
  CategoriaConceito,
  CriarSnippetRequest,
  Pagina,
  SnippetDTO,
} from '@/types/api'

export const snippetsApi = {
  listarMeus: (pagina: number, tamanho: number, categoria?: CategoriaConceito | null) =>
    api
      .get<Pagina<SnippetDTO>>('/api/snippets', {
        params: { pagina, tamanho, ...(categoria ? { categoria } : {}) },
      })
      .then((r) => r.data),

  listarDoDesafio: (desafioId: string, pagina: number, tamanho: number) =>
    api
      .get<Pagina<SnippetDTO>>('/api/snippets', {
        params: { desafioId, pagina, tamanho },
      })
      .then((r) => r.data),

  buscarDetalhe: (id: string) => api.get<SnippetDTO>(`/api/snippets/${id}`).then((r) => r.data),

  criar: (body: CriarSnippetRequest) =>
    api.post<SnippetDTO>('/api/snippets', body).then((r) => r.data),

  atualizar: (id: string, body: AtualizarSnippetRequest) =>
    api.patch<void>(`/api/snippets/${id}`, body).then((r) => r.data),

  remover: (id: string) => api.delete<void>(`/api/snippets/${id}`).then((r) => r.data),
}
