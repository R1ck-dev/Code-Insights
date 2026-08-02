import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { consentimentoApi, pesquisaApi } from './api'

export const pesquisaKeys = {
  all: ['pesquisa'] as const,
  qualidade: () => ['pesquisa', 'qualidade'] as const,
  coorte: () => ['pesquisa', 'coorte'] as const,
  consentimento: () => ['consentimento'] as const,
}

/**
 * Carregado no shell autenticado, porque o aviso de "responda ao termo" aparece em qualquer tela.
 * O texto do termo vem junto — é um arquivo, não muda entre requisições, e buscá-lo à parte só
 * criaria a chance de a tela mostrar um texto e gravar a resposta sob outra versão.
 */
export function useMeuConsentimento() {
  return useQuery({
    queryKey: pesquisaKeys.consentimento(),
    queryFn: () => consentimentoApi.meu(),
  })
}

export function useDecidirConsentimento() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (autoriza: boolean) => consentimentoApi.decidir(autoriza),
    onSuccess: (atualizado) => {
      queryClient.setQueryData(pesquisaKeys.consentimento(), atualizado)
      // A coorte muda de tamanho com a decisão — sem invalidar, a tela do pesquisador seguiria
      // mostrando quem acabou de revogar.
      queryClient.invalidateQueries({ queryKey: pesquisaKeys.all })
    },
  })
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
