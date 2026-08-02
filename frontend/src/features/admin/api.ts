import { api } from '@/lib/api'
import type {
  ContaAtivadaDTO,
  LinkRedefinicaoResponse,
  Pagina,
  PapelAlteradoDTO,
  UsuarioAdminDTO,
} from '@/types/api'

/**
 * As três ações identificam a pessoa por **e-mail**, e não por id. É o contrato que o backend já
 * tinha; a tela apenas deixa de exigir que alguém o digite de memória — o e-mail sai da linha da
 * listagem.
 */
export const adminApi = {
  listarUsuarios: (busca: string, pagina: number, tamanho = 20) =>
    api
      .get<Pagina<UsuarioAdminDTO>>('/api/admin/usuarios', {
        params: { busca, pagina, tamanho },
      })
      .then((r) => r.data),

  promoverParaPesquisador: (email: string) =>
    api
      .post<PapelAlteradoDTO>('/api/admin/usuarios/promover-pesquisador', { email })
      .then((r) => r.data),

  ativarConta: (email: string) =>
    api.post<ContaAtivadaDTO>('/api/admin/usuarios/ativar', { email }).then((r) => r.data),

  gerarLinkDeRedefinicao: (email: string) =>
    api
      .post<LinkRedefinicaoResponse>('/api/admin/usuarios/link-redefinicao-senha', { email })
      .then((r) => r.data),
}
