import { api } from '@/lib/api'
import type {
  EvolucaoMensalDTO,
  GranularidadeTempo,
  ResultadoMetricaDTO,
  ResumoDashboardDTO,
} from '@/types/api'

export const metricasApi = {
  listarDaResolucao: (resolucaoId: string) =>
    api.get<ResultadoMetricaDTO[]>(`/api/resolucoes/${resolucaoId}/metricas`).then((r) => r.data),

  resumoDashboard: () =>
    api.get<ResumoDashboardDTO>('/api/metricas/resumo').then((r) => r.data),

  evolucao: (granularidade: GranularidadeTempo) =>
    api
      .get<EvolucaoMensalDTO[]>('/api/metricas/evolucao', { params: { granularidade } })
      .then((r) => r.data),
}
