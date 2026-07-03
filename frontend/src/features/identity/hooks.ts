import { useMutation, useQuery } from '@tanstack/react-query'
import { identityApi } from './api'
import type {
  AtualizarMeuPerfilRequest,
  RegistrarUsuarioRequest,
} from '@/types/api'

export const identityKeys = {
  usuarioPublico: (id: string) => ['usuario-publico', id] as const,
  ativacao: (token: string) => ['ativacao', token] as const,
}

export function useUsuarioPublico(id: string | undefined) {
  return useQuery({
    queryKey: identityKeys.usuarioPublico(id ?? ''),
    queryFn: () => identityApi.buscarUsuarioPublico(id!),
    enabled: !!id,
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
