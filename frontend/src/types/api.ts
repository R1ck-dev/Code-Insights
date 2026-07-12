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
/**
 * Resumo de uma resolução para LISTAS (as resoluções de um desafio). Traz a complexidade
 * de **tempo** — e só ela: é nesta lista que o aluno compara suas tentativas lado a lado.
 * Espaço e ciclomática ficam no detalhe (`ResultadoMetricaDTO`).
 *
 * Mesmas regras de leitura do `PontoCartaDTO` (ver abaixo), que valem para TODO campo de
 * métrica da API:
 *  - `null` = **não há dado** (`analisada === false`, ou linguagem sem analisador — hoje só Java).
 *  - `tempoOrdem === -1` = **DESCONHECIDO**: o motor rodou e não classificou (rótulo `"?"`).
 *    Semanticamente DIFERENTE de `null`.
 *  - ⚠ `0` é `O(1)` — complexidade legítima, nunca sentinela de vazio. Teste `== null` e
 *    `=== -1` explicitamente, nunca `if (!tempoOrdem)`.
 *  - `confiancaTempo` é a confiança do MOTOR, **não** o eixo MEDIDO × ≈ ESTIMADO. Big-O é
 *    sempre ≈ ESTIMADO (`TIPO_METRICA_META`), inclusive com confiança `ALTA`.
 */
export interface ResolucaoResumoDTO {
  id: string
  desafioId: string
  autorId: string
  linguagem: LinguagemProgramacao
  indiceAutonomiaIA: number
  visibilidade: Visibilidade
  analisada: boolean
  /** Ex.: `"O(n log n)"`, ou `"?"` quando `tempoOrdem === -1`. */
  tempoRotulo: string | null
  /** `k` do colormap: 0..7; `-1` = desconhecido; `null` = sem dado. */
  tempoOrdem: number | null
  confiancaTempo: NivelConfianca | null
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
 *
 * ⚠ NÃO É O EIXO MEDIDO × ≈ ESTIMADO. `ALTA` significa "o motor reconheceu TODOS os
 * construtos do código" — não significa que o Big O foi MEDIDO. Inferir a complexidade de um
 * código arbitrário é indecidível no caso geral: Big O de tempo/espaço é SEMPRE uma
 * estimativa, com qualquer nível de confiança. Quem decide MEDIDO/≈ESTIMADO é o TIPO da
 * métrica (`TIPO_METRICA_META` em `@/domain/enums`), e só ele.
 * Este nível é informação SECUNDÁRIA: vira texto ("confiança do motor: alta"), nunca preenche
 * um marcador nem tira o `≈` de um valor.
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

// ---- Knowledge: Carta celeste ----
/**
 * Uma estrela da carta celeste: uma resolução do autor, posicionada por Índice de
 * Autonomia IA (eixo X, 1..5) e classe de Big O de tempo (eixo Y).
 * Espelha `application/knowledge/dto/PontoCartaDTO.java`.
 *
 * Regras de leitura dos campos de métrica (`tempo*`, `espaco*`, `ciclomatica`):
 *
 * - `null` = **não há dado**. É o estado enquanto `analisada === false`, e continua
 *   `null` para sempre quando a linguagem não tem analisador — hoje **só Java** produz
 *   métricas de complexidade (ver §4.4 do contrato de design). `null` ≠ "erro": é ausência.
 * - `tempoOrdem`/`espacoOrdem` são a **ordem** da classe (`k`) na escala de complexidade:
 *   `0 = O(1)`, `1 = O(log n)`, `2 = O(n)`, `3 = O(n log n)`, `4 = O(n²)`, `5 = O(n³)`,
 *   `6 = O(2ⁿ)`, `7 = O(n!)`. O backend normaliza tudo para exatamente essas 8 classes,
 *   então `ordem` já é o `k` do colormap — sem tradução no front.
 * - **`ordem === -1` significa DESCONHECIDO** (rótulo `"?"`): o motor rodou e **não
 *   conseguiu classificar**. É semanticamente DIFERENTE de `null` (não há dado).
 * - ⚠ **`0` é uma complexidade legítima — `O(1)`, a melhor delas.** Nunca trate `0` como
 *   sentinela de "vazio"/"sem métrica": teste sempre `== null` e `=== -1` explicitamente,
 *   nunca `if (!tempoOrdem)`.
 * - Para plotar: só entram pontos com `ordem != null && ordem >= 0`. Os demais contam no
 *   rodapé "N de M resoluções plotadas", decompostos por motivo (calculando · sem analisador ·
 *   não classificada).
 * - ⚠ `confiancaTempo` **NÃO** é MEDIDO × ≈ ESTIMADO. É a confiança do MOTOR na própria
 *   estimativa (ver `NivelConfianca`, acima). Big O é SEMPRE ≈ ESTIMADO, inclusive com
 *   confiança ALTA. Mapear `ALTA → MEDIDO` faz o gráfico AFIRMAR que a estimativa foi medida —
 *   o oposto da regra 3 (nunca esconder a incerteza da métrica).
 */
export interface PontoCartaDTO {
  resolucaoId: string
  desafioId: string
  /** Título do desafio — as constelações ligam resoluções do mesmo `desafioId`. */
  desafioTitulo: string
  linguagem: LinguagemProgramacao
  /** Autodeclarado, 1..5 (1 = mais apoio de IA, 5 = mais autônomo). Nunca nulo. */
  indiceAutonomiaIA: number
  /** `false` = análise ainda não rodou; todas as métricas abaixo vêm `null`. */
  analisada: boolean
  /** Ex.: `"O(n log n)"`, ou `"?"` quando `tempoOrdem === -1`. */
  tempoRotulo: string | null
  /** `k` do colormap: 0..7; `-1` = desconhecido; `null` = sem dado. */
  tempoOrdem: number | null
  confiancaTempo: NivelConfianca | null
  espacoRotulo: string | null
  /** `k` do colormap: 0..7; `-1` = desconhecido; `null` = sem dado. */
  espacoOrdem: number | null
  /** Complexidade ciclomática (McCabe). Contagem de caminhos — não é escala de colormap. */
  ciclomatica: number | null
  visibilidade: Visibilidade
  submetidaEm: string
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
