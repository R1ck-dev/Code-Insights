/*
 * Tipos da API do CodeInsights — espelham 1:1 os DTOs e enums do backend
 * (com.projeto.codeinsights). Fonte: docs/design/api-reference/mapa-atributos-api.md.
 */

// ---- Enums (valores reais do backend) ----
export type Role = 'ALUNO' | 'PESQUISADOR' | 'ADMIN'

export type StatusConta = 'PENDENTE_VERIFICACAO' | 'ATIVO' | 'INATIVO' | 'SUSPENSO'

export type Visibilidade = 'PUBLICO' | 'PRIVADO'

export type LinguagemProgramacao = 'JAVA' | 'PYTHON' | 'CPP' | 'JAVASCRIPT' | 'C'

export type CategoriaConceito =
  | 'ESTRUTURA_DADOS'
  | 'RECURSAO'
  | 'ORDENACAO'
  | 'GRAFOS'
  | 'PROGRAMACAO_DINAMICA'
  | 'STRINGS'
  | 'MATEMATICA'

export type TipoMetrica = 'COMPLEXIDADE_CICLOMATICA' | 'BIG_O_TEMPO' | 'COMPLEXIDADE_ESPACO'

// ---- Envelope de paginação ----
export interface Pagina<T> {
  itens: T[]
  paginaAtual: number
  totalPaginas: number
  totalItens: number
}

// ---- Identity ----
export interface TokenResponse {
  token: string
  tipo: string
}

export interface MeuPerfilDTO {
  id: string
  username: string
  email: string
  role: Role
  status: StatusConta
  visibilidadePerfil: Visibilidade
  criadoEm: string
}

export interface UsuarioPublicoDTO {
  id: string
  username: string
  visibilidadePerfil: Visibilidade
}

// ---- Knowledge: Desafios ----
export interface DesafioResumoDTO {
  id: string
  autorId: string
  autorUsername: string
  titulo: string
  plataformaOrigem: string | null
  visibilidade: Visibilidade
  criadoEm: string
}

export interface DesafioDetalheDTO {
  id: string
  autorId: string
  autorUsername: string
  titulo: string
  enunciado: string | null
  plataformaOrigem: string | null
  identificadorExterno: string | null
  urlExterna: string | null
  visibilidade: Visibilidade
  qtdResolucoes: number
  criadoEm: string
  atualizadoEm: string
}

// ---- Knowledge: Resoluções ----
export interface ResolucaoResumoDTO {
  id: string
  desafioId: string
  autorId: string
  linguagem: LinguagemProgramacao
  indiceAutonomiaIA: number
  analisada: boolean
  submetidaEm: string
}

export interface ResolucaoDetalheDTO {
  id: string
  desafioId: string
  autorId: string
  codigoFonte: string
  linguagem: LinguagemProgramacao
  indiceAutonomiaIA: number
  descricaoApoioIA: string | null
  visibilidade: Visibilidade
  analisada: boolean
  submetidaEm: string
}

// ---- Knowledge: Métricas ----
export interface ResultadoMetricaDTO {
  tipo: TipoMetrica
  valor: number
  rotulo: string
  detalhe: string | null
  analisadoEm: string
}

// ---- Knowledge: Snippets ----
export interface SnippetDTO {
  id: string
  autorId: string
  codigo: string
  descricao: string | null
  categoria: CategoriaConceito
  criadoEm: string
}

// ---- Payloads de request ----
export interface RegistrarUsuarioRequest {
  username: string
  email: string
  password: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface AtualizarMeuPerfilRequest {
  username: string
}

export interface AlterarVisibilidadePerfilRequest {
  publico: boolean
}

export interface CriarDesafioRequest {
  titulo: string
  enunciado?: string | null
  plataformaOrigem?: string | null
  identificadorExterno?: string | null
  urlExterna?: string | null
}

export type AtualizarDesafioRequest = Partial<CriarDesafioRequest>

export interface AlterarVisibilidadeDesafioRequest {
  publico: boolean
}

export interface SubmeterResolucaoRequest {
  codigoFonte: string
  linguagem: LinguagemProgramacao
  indiceAutonomiaIA: number
  descricaoApoioIA?: string | null
}

export interface AtualizarResolucaoRequest {
  codigoFonte?: string
  linguagem?: LinguagemProgramacao
}

export interface CriarSnippetRequest {
  codigo: string
  descricao?: string | null
  categoria: CategoriaConceito
}

export interface AtualizarSnippetRequest {
  codigo?: string
  descricao?: string | null
  categoria?: CategoriaConceito
}
