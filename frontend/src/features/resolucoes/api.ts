import { api } from '@/lib/api'
import type {
  Pagina,
  ResolucaoDetalheDTO,
  ResolucaoResumoDTO,
  SubmeterResolucaoRequest,
} from '@/types/api'

export const resolucoesApi = {
  submeter: (desafioId: string, body: SubmeterResolucaoRequest) =>
    api
      .post<ResolucaoResumoDTO>(`/api/desafios/${desafioId}/resolucoes`, body)
      .then((r) => r.data),

  listarDoDesafio: (desafioId: string, pagina: number, tamanho: number) =>
    api
      .get<Pagina<ResolucaoResumoDTO>>(`/api/desafios/${desafioId}/resolucoes`, {
        params: { pagina, tamanho },
      })
      .then((r) => r.data),

  buscarDetalhe: (id: string) =>
    api.get<ResolucaoDetalheDTO>(`/api/resolucoes/${id}`).then((r) => r.data),

  alterarVisibilidade: (id: string, publico: boolean) =>
    api.patch<void>(`/api/resolucoes/${id}/visibilidade`, { publico }).then((r) => r.data),

  remover: (id: string) => api.delete<void>(`/api/resolucoes/${id}`).then((r) => r.data),
}
