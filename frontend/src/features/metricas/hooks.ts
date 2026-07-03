import { useQuery } from '@tanstack/react-query'
import { metricasApi } from './api'

export const metricasKeys = {
  daResolucao: (resolucaoId: string) => ['metricas', resolucaoId] as const,
}

export function useMetricasDaResolucao(
  resolucaoId: string | undefined,
  opts?: { refetchInterval?: number | false; enabled?: boolean },
) {
  return useQuery({
    queryKey: metricasKeys.daResolucao(resolucaoId ?? ''),
    queryFn: () => metricasApi.listarDaResolucao(resolucaoId!),
    enabled: (opts?.enabled ?? true) && !!resolucaoId,
    refetchInterval: opts?.refetchInterval ?? false,
  })
}
