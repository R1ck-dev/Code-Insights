import { api } from '@/lib/api'
import type { ResultadoMetricaDTO } from '@/types/api'

export const metricasApi = {
  listarDaResolucao: (resolucaoId: string) =>
    api.get<ResultadoMetricaDTO[]>(`/api/resolucoes/${resolucaoId}/metricas`).then((r) => r.data),
}
