import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { resolucoesApi } from './api'
import { desafiosKeys } from '@/features/desafios/hooks'
import { metricasKeys } from '@/features/metricas/hooks'
import type {
  AtualizarResolucaoRequest,
  ResolucaoDetalheDTO,
  SubmeterResolucaoRequest,
} from '@/types/api'

export const resolucoesKeys = {
  all: ['resolucoes'] as const,
  doDesafio: (desafioId: string, pagina: number, tamanho: number) =>
    ['resolucoes', 'desafio', desafioId, pagina, tamanho] as const,
  detalhe: (id: string) => ['resolucoes', 'detalhe', id] as const,
}

export function useResolucoesDoDesafio(desafioId: string | undefined, pagina: number, tamanho = 10) {
  return useQuery({
    queryKey: resolucoesKeys.doDesafio(desafioId ?? '', pagina, tamanho),
    queryFn: () => resolucoesApi.listarDoDesafio(desafioId!, pagina, tamanho),
    enabled: !!desafioId,
    placeholderData: keepPreviousData,
  })
}

export function useResolucaoDetalhe(id: string | undefined) {
  return useQuery({
    queryKey: resolucoesKeys.detalhe(id ?? ''),
    queryFn: () => resolucoesApi.buscarDetalhe(id!),
    enabled: !!id,
    // Enquanto a análise assíncrona não terminou, refaz a busca a cada 4s
    // para "analisada" virar true e as métricas aparecerem.
    refetchInterval: (query) => {
      const data = query.state.data as ResolucaoDetalheDTO | undefined
      return data && !data.analisada ? 4000 : false
    },
  })
}

export function useSubmeterResolucao(desafioId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: SubmeterResolucaoRequest) => resolucoesApi.submeter(desafioId, body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: resolucoesKeys.all })
      void qc.invalidateQueries({ queryKey: desafiosKeys.detalhe(desafioId) })
    },
  })
}

export function useAtualizarResolucao(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: AtualizarResolucaoRequest) => resolucoesApi.atualizar(id, body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: resolucoesKeys.detalhe(id) })
      void qc.invalidateQueries({ queryKey: metricasKeys.daResolucao(id) })
    },
  })
}

export function useAlterarVisibilidadeResolucao(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (publico: boolean) => resolucoesApi.alterarVisibilidade(id, publico),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: resolucoesKeys.detalhe(id) })
      void qc.invalidateQueries({ queryKey: resolucoesKeys.all })
    },
  })
}

export function useRemoverResolucao() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => resolucoesApi.remover(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: resolucoesKeys.all }),
  })
}
