import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi } from './api'

export const adminKeys = {
  all: ['admin'] as const,
  usuarios: (busca: string, pagina: number) => ['admin', 'usuarios', busca, pagina] as const,
}

/**
 * `keepPreviousData` mantém a lista na tela enquanto a busca seguinte carrega. Sem isso, cada letra
 * digitada esvaziaria a tabela e a página saltaria — e o admin estaria procurando alguém enquanto o
 * conteúdo pisca.
 */
export function useUsuariosAdmin(busca: string, pagina: number) {
  return useQuery({
    queryKey: adminKeys.usuarios(busca, pagina),
    queryFn: () => adminApi.listarUsuarios(busca, pagina),
    placeholderData: keepPreviousData,
  })
}

/**
 * As duas mutações que mudam o estado da conta invalidam a listagem: o papel e o status aparecem na
 * linha, e deixá-los desatualizados faria o admin repetir a ação achando que não pegou.
 */
export function usePromoverParaPesquisador() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (email: string) => adminApi.promoverParaPesquisador(email),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminKeys.all }),
  })
}

export function useAtivarConta() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (email: string) => adminApi.ativarConta(email),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminKeys.all }),
  })
}

/** Não invalida nada: gerar link não altera a conta, só produz um token de uso único. */
export function useGerarLinkDeRedefinicao() {
  return useMutation({
    mutationFn: (email: string) => adminApi.gerarLinkDeRedefinicao(email),
  })
}
