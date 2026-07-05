import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { desafiosApi } from './api'
import type { AtualizarDesafioRequest, CriarDesafioRequest } from '@/types/api'

export const desafiosKeys = {
  all: ['desafios'] as const,
  meus: (pagina: number, tamanho: number) => ['desafios', 'meus', pagina, tamanho] as const,
  publicosDoAutor: (autorId: string, pagina: number, tamanho: number) =>
    ['desafios', 'autor', autorId, pagina, tamanho] as const,
  detalhe: (id: string) => ['desafios', 'detalhe', id] as const,
}

export function useMeusDesafios(pagina: number, tamanho = 12) {
  return useQuery({
    queryKey: desafiosKeys.meus(pagina, tamanho),
    queryFn: () => desafiosApi.listarMeus(pagina, tamanho),
    placeholderData: keepPreviousData,
  })
}

export function useDesafiosPublicosDoAutor(autorId: string, pagina: number, tamanho = 12) {
  return useQuery({
    queryKey: desafiosKeys.publicosDoAutor(autorId, pagina, tamanho),
    queryFn: () => desafiosApi.listarPublicosDoAutor(autorId, pagina, tamanho),
    enabled: !!autorId,
    placeholderData: keepPreviousData,
  })
}

export function useDesafioDetalhe(id: string | undefined) {
  return useQuery({
    queryKey: desafiosKeys.detalhe(id ?? ''),
    queryFn: () => desafiosApi.buscarDetalhe(id!),
    enabled: !!id,
  })
}

export function useCriarDesafio() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CriarDesafioRequest) => desafiosApi.criar(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: desafiosKeys.all }),
  })
}

export function useAtualizarDesafio(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: AtualizarDesafioRequest) => desafiosApi.atualizar(id, body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: desafiosKeys.detalhe(id) })
      void qc.invalidateQueries({ queryKey: desafiosKeys.all })
    },
  })
}

export function useAlterarVisibilidadeDesafio(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (publico: boolean) => desafiosApi.alterarVisibilidade(id, publico),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: desafiosKeys.detalhe(id) })
      void qc.invalidateQueries({ queryKey: desafiosKeys.all })
    },
  })
}

export function useRemoverDesafio() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => desafiosApi.remover(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: desafiosKeys.all }),
  })
}
