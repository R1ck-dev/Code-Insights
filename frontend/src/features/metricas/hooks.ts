import { useQuery } from '@tanstack/react-query'
import { metricasApi } from './api'

export const metricasKeys = {
  daResolucao: (resolucaoId: string) => ['metricas', resolucaoId] as const,
  resumo: ['metricas', 'resumo'] as const,
  carta: ['metricas', 'carta'] as const,
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

/**
 * Carta celeste do aluno logado: uma estrela por resolução (autonomia × Big O de tempo).
 * Dataset das 5 visualizações do dashboard. Só plotam os pontos com `tempoOrdem >= 0`;
 * `null` (sem métrica) e `-1` (motor não classificou) contam no rodapé "sem métrica".
 */
export function useCartaCeleste() {
  return useQuery({
    queryKey: metricasKeys.carta,
    queryFn: () => metricasApi.carta(),
  })
}

/*
 * ⚠ NÃO existe `useEvolucao`. O endpoint `/api/metricas/evolucao` continua no `metricasApi`,
 * mas nenhuma tela o consome: a Linha temporal deriva os buckets mensais da CARTA, no cliente
 * (`bucketsMensais(dataset.todas)`), porque precisa das resoluções SEM métrica para a série de
 * autonomia — coisa que o agregado do backend não devolve. Um hook sem consumidor só serve
 * para driftar; se um dia a Linha passar a ler o endpoint, o hook volta junto.
 */
