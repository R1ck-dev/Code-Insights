import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query'
import { identityApi } from './api'
import type {
  AtualizarMeuPerfilRequest,
  RegistrarUsuarioRequest,
} from '@/types/api'

export const identityKeys = {
  usuarioPublico: (id: string) => ['usuario-publico', id] as const,
  usuariosPublicos: (filtro: string, pagina: number, tamanho: number) =>
    ['usuarios-publicos', filtro, pagina, tamanho] as const,
  ativacao: (token: string) => ['ativacao', token] as const,
}

export function useUsuarioPublico(id: string | undefined) {
  return useQuery({
    queryKey: identityKeys.usuarioPublico(id ?? ''),
    queryFn: () => identityApi.buscarUsuarioPublico(id!),
    enabled: !!id,
  })
}

export function useUsuariosPublicos(filtro: string, pagina: number, tamanho = 12) {
  return useQuery({
    queryKey: identityKeys.usuariosPublicos(filtro, pagina, tamanho),
    queryFn: () => identityApi.listarPublicos(filtro, pagina, tamanho),
    placeholderData: keepPreviousData,
  })
}

/** Ativação de conta a partir do token do e-mail (GET dispara a ativação no backend). */
export function useAtivarConta(token: string | null) {
  return useQuery({
    queryKey: identityKeys.ativacao(token ?? ''),
    queryFn: () => identityApi.ativarConta(token!),
    enabled: !!token,
    retry: false,
    staleTime: Infinity,
  })
}

export function useRegistrar() {
  return useMutation({
    mutationFn: (body: RegistrarUsuarioRequest) => identityApi.registrar(body),
  })
}

export function useAtualizarPerfil() {
  return useMutation({
    mutationFn: (body: AtualizarMeuPerfilRequest) => identityApi.atualizarMeuPerfil(body),
  })
}

export function useAlterarVisibilidadePerfil() {
  return useMutation({
    mutationFn: (publico: boolean) => identityApi.alterarVisibilidade({ publico }),
  })
}

export function useEsqueciSenha() {
  return useMutation({ mutationFn: (email: string) => identityApi.esqueciSenha(email) })
}

export function useRedefinirSenha() {
  return useMutation({
    mutationFn: (body: { token: string; novaSenha: string }) => identityApi.redefinirSenha(body),
  })
}

export function useReenviarAtivacao() {
  return useMutation({ mutationFn: (email: string) => identityApi.reenviarAtivacao(email) })
}
