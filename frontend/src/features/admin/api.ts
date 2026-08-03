import { api } from '@/lib/api'
import type {
  ContaAtivadaDTO,
  LinkRedefinicaoResponse,
  Pagina,
  PapelAlteradoDTO,
  RelatorioReanaliseDTO,
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

  /**
   * Sem `autorId`: o corpus inteiro. O endpoint aceita restringir a um autor, mas a tela não expõe
   * isso — reanalisar meia plataforma deixaria métricas de motores diferentes convivendo no mesmo
   * dado de pesquisa, que é justamente o que a operação existe para desfazer.
   *
   * Síncrono: a resposta é o relatório da passada.
   */
  reanalisarMetricas: () =>
    api.post<RelatorioReanaliseDTO>('/api/admin/metricas/reanalisar').then((r) => r.data),
}
