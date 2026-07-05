import { useQuery } from '@tanstack/react-query'
import { metricasApi } from './api'

export const metricasKeys = {
  daResolucao: (resolucaoId: string) => ['metricas', resolucaoId] as const,
  resumo: ['metricas', 'resumo'] as const,
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

/** Agregados do dashboard do aluno logado (totais, autonomia média, distribuições, evolução, atividade recente). */
export function useResumoDashboard() {
  return useQuery({
    queryKey: metricasKeys.resumo,
    queryFn: () => metricasApi.resumoDashboard(),
  })
}
