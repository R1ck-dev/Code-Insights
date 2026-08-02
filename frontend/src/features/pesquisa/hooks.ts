import { useMutation, useQuery } from '@tanstack/react-query'
import { pesquisaApi } from './api'

export const pesquisaKeys = {
  all: ['pesquisa'] as const,
  qualidade: () => ['pesquisa', 'qualidade'] as const,
  coorte: () => ['pesquisa', 'coorte'] as const,
}

export function useQualidadeDaCoorte() {
  return useQuery({
    queryKey: pesquisaKeys.qualidade(),
    queryFn: () => pesquisaApi.qualidade(),
  })
}

export function useCoorte() {
  return useQuery({
    queryKey: pesquisaKeys.coorte(),
    queryFn: () => pesquisaApi.listarCoorte(),
  })
}

/**
 * Baixar não é mutação de estado no servidor, mas usa `useMutation` porque é uma ação disparada por
 * clique com pendente e erro próprios — `useQuery` buscaria sozinho ao montar a tela e geraria o
 * arquivo sem ninguém pedir.
 */
export function useExportarCsv() {
  return useMutation({
    mutationFn: () => pesquisaApi.exportarCsv(),
    onSuccess: ({ blob, nomeDoArquivo }) => baixar(blob, nomeDoArquivo),
  })
}

export function useIdentificarParticipante() {
  return useMutation({
    mutationFn: (pseudonimo: string) => pesquisaApi.identificar(pseudonimo),
  })
}

/** Dispara o download no navegador e libera a URL temporária — sem revoke, o blob vaza na aba. */
function baixar(blob: Blob, nomeDoArquivo: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = nomeDoArquivo
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
