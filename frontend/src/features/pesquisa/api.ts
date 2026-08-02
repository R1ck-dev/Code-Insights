import { api } from '@/lib/api'
import type {
  MeuConsentimentoDTO,
  ParticipanteIdentificadoDTO,
  QualidadeDaCoorteDTO,
  ResolucaoDaCoorteDTO,
} from '@/types/api'

/**
 * Fora de `/api/pesquisa/**` de propósito: aquele prefixo é restrito a PESQUISADOR e ADMIN no
 * `SecurityConfig`, e quem responde ao termo é o aluno.
 */
export const consentimentoApi = {
  meu: () => api.get<MeuConsentimentoDTO>('/api/consentimento').then((r) => r.data),

  decidir: (autoriza: boolean) =>
    api.post<MeuConsentimentoDTO>('/api/consentimento', { autoriza }).then((r) => r.data),
}

export const pesquisaApi = {
  qualidade: () => api.get<QualidadeDaCoorteDTO>('/api/pesquisa/qualidade').then((r) => r.data),

  listarCoorte: () =>
    api.get<ResolucaoDaCoorteDTO[]>('/api/pesquisa/coorte').then((r) => r.data),

  /**
   * O CSV vem como `blob` porque `responseType` padrão é JSON — sem isto o axios tentaria parsear o
   * arquivo e devolveria string corrompida. O nome sugerido vem do `Content-Disposition`, para que
   * a data do export seja a do servidor e não a do relógio do navegador.
   */
  exportarCsv: () =>
    api.get('/api/pesquisa/coorte.csv', { responseType: 'blob' }).then((r) => ({
      blob: r.data as Blob,
      nomeDoArquivo: nomeDoArquivoDe(r.headers['content-disposition']),
    })),

  identificar: (pseudonimo: string) =>
    api
      .post<ParticipanteIdentificadoDTO>('/api/pesquisa/participantes/identificar', { pseudonimo })
      .then((r) => r.data),
}

const PADRAO = 'codeinsights-coorte.csv'

function nomeDoArquivoDe(contentDisposition: unknown): string {
  if (typeof contentDisposition !== 'string') return PADRAO
  return /filename="([^"]+)"/.exec(contentDisposition)?.[1] ?? PADRAO
}
