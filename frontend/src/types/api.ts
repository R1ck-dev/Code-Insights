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

export type GranularidadeTempo = 'DIARIO' | 'SEMANAL' | 'MENSAL'

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
  visibilidade: Visibilidade
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
/**
 * Confiança do motor no valor que ele mesmo calculou. ALTA = nenhuma suposição;
 * MEDIA = ao menos um default conservador ("assumi que este laço roda n vezes");
 * BAIXA = não foi possível classificar (rótulo "?").
 */
export type NivelConfianca = 'ALTA' | 'MEDIA' | 'BAIXA'

export interface ResultadoMetricaDTO {
  tipo: TipoMetrica
  valor: number
  rotulo: string
  detalhe: string | null
  confianca: NivelConfianca
  analisadoEm: string
}

// ---- Knowledge: Dashboard (agregados) ----
export interface DistribuicaoItemDTO {
  rotulo: string
  /** ordinal da classe de complexidade (0 = O(1) … 7 = O(n!)). */
  ordem: number
  total: number
}

export interface EvolucaoMensalDTO {
  ano: number
  mes: number
  /** dia do início do período (1 quando mensal; início da semana quando semanal). */
  dia: number
  mediaAutonomia: number | null
  totalResolucoes: number
  /** média da ordem de Big O (tempo) das resoluções do período; null se nenhuma analisada. */
  mediaComplexidade: number | null
}

export interface AtividadeRecenteDTO {
  resolucaoId: string
  desafioId: string
  desafioTitulo: string
  linguagem: LinguagemProgramacao
  indiceAutonomiaIA: number
  analisada: boolean
  /** rótulo/ordinal de Big O (tempo); null se não analisada ou sem métrica de tempo. */
  complexidadeRotulo: string | null
  complexidadeOrdem: number | null
  submetidaEm: string
}

export interface ResumoDashboardDTO {
  totalDesafios: number
  desafiosPublicos: number
  totalResolucoes: number
  resolucoesAnalisadas: number
  totalSnippets: number
  totalCategorias: number
  mediaAutonomia: number | null
  distribuicaoBigO: DistribuicaoItemDTO[]
  distribuicaoEspaco: DistribuicaoItemDTO[]
  evolucao: EvolucaoMensalDTO[]
  atividadeRecente: AtividadeRecenteDTO[]
}

// ---- Knowledge: Snippets ----
export interface SnippetDTO {
  id: string
  autorId: string
  desafioId: string | null
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

export interface CriarSnippetRequest {
  codigo: string
  descricao?: string | null
  categoria: CategoriaConceito
  desafioId?: string | null
}

export interface AtualizarSnippetRequest {
  codigo?: string
  descricao?: string | null
  categoria?: CategoriaConceito
}
