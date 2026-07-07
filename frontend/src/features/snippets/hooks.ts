import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { snippetsApi } from './api'
import type { AtualizarSnippetRequest, CategoriaConceito, CriarSnippetRequest } from '@/types/api'

export const snippetsKeys = {
  all: ['snippets'] as const,
  meus: (pagina: number, tamanho: number, categoria: CategoriaConceito | null) =>
    ['snippets', 'meus', pagina, tamanho, categoria] as const,
  doDesafio: (desafioId: string) => ['snippets', 'desafio', desafioId] as const,
}

export function useMeusSnippets(
  pagina: number,
  categoria: CategoriaConceito | null = null,
  tamanho = 12,
) {
  return useQuery({
    queryKey: snippetsKeys.meus(pagina, tamanho, categoria),
    queryFn: () => snippetsApi.listarMeus(pagina, tamanho, categoria),
    placeholderData: keepPreviousData,
  })
}

export function useSnippetsDoDesafio(desafioId: string | undefined, tamanho = 50) {
  return useQuery({
    queryKey: snippetsKeys.doDesafio(desafioId ?? ''),
    queryFn: () => snippetsApi.listarDoDesafio(desafioId!, 0, tamanho),
    enabled: !!desafioId,
  })
}

export function useCriarSnippet() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CriarSnippetRequest) => snippetsApi.criar(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: snippetsKeys.all }),
  })
}

export function useAtualizarSnippet(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: AtualizarSnippetRequest) => snippetsApi.atualizar(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: snippetsKeys.all }),
  })
}

export function useRemoverSnippet() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => snippetsApi.remover(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: snippetsKeys.all }),
  })
}
