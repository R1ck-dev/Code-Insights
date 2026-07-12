/*
 * MODELO DE DADOS PLOTÁVEL — o contrato dos 5 gráficos (Carta, Órbitas, Espectro,
 * Linha, Matriz). Todos leem do MESMO dataset (spec 02 §0.4): um ponto = uma resolução.
 *
 * `PontoPlotavel` é o `PontoCartaDTO` (types/api.ts) já RESOLVIDO para desenho:
 *   - `k` é uma classe real do colormap (0..7) — quem não tem classe de tempo não vira
 *     ponto (ver `dataset.ts`, regra de plotabilidade);
 *   - `submetidaEm` já é `Date` (a ordem cronológica é dado: ângulo das Órbitas,
 *     sequência das constelações, buckets da Linha).
 *
 * ⚠ MEDIDO × ≈ ESTIMADO é propriedade do TIPO da métrica, não do valor:
 *   Big-O de tempo/espaço é SEMPRE ≈ ESTIMADO (inferência estática, indecidível no caso
 *   geral); a ciclomática é SEMPRE MEDIDA (contagem no AST). A fonte única é
 *   `TIPO_METRICA_META` (`@/domain/enums`). `confiancaTempo` aqui é OUTRO eixo: a confiança
 *   do MOTOR no valor que ele mesmo estimou (ALTA/MEDIA/BAIXA) — vira texto, NUNCA preenche
 *   um marcador. Confundir os dois eixos é afirmar que uma estimativa foi medida.
 *
 * ⚠ `k === 0` é O(1) — a MELHOR classe, não "vazio". Nunca `if (!k)`.
 */
import type { GranularidadeTempo, LinguagemProgramacao, NivelConfianca, Visibilidade } from '@/types/api'
import type { ClasseK, Tema } from '@/domain/enums'

/** Índice de Autonomia IA — autodeclarado, 1 (mais apoio de IA) a 5 (mais autônomo). */
export type NivelAutonomia = 1 | 2 | 3 | 4 | 5

/** Menor e maior nível da escala de autonomia (eixo X da Carta, raio das Órbitas). */
export const AUTONOMIA_MIN = 1
export const AUTONOMIA_MAX = 5
/** 5 colunas: a escala de autonomia é discreta e completa. */
export const TOTAL_AUTONOMIA = 5

/**
 * TODA resolução do portfólio, plotável ou não. Existe porque a AUTONOMIA é autodeclarada e
 * INDEPENDE da linguagem (§4.4): uma resolução em Python não vira estrela, mas continua sendo
 * trabalho feito naquele mês — descartá-la da série de autonomia (ou do "quantas resoluções
 * teve este mês") transformaria "não sei medir" em "não aconteceu".
 */
export interface PontoBase {
  resolucaoId: string
  /** Agrupador das constelações: resoluções do mesmo desafio formam uma trajetória. */
  desafioId: string
  desafioTitulo: string
  linguagem: LinguagemProgramacao
  /** Eixo X da Carta · raio das Órbitas · colunas da Matriz. NEUTRA — nunca colormap. */
  autonomia: NivelAutonomia
  /** `false` = a análise assíncrona ainda não rodou (≠ "não tem métrica"). */
  analisada: boolean
  /** Classe de TEMPO (0..7) quando plotável; `null` quando não há classe. */
  k: ClasseK | null
  /** Já parseada. Ordem cronológica ascendente é garantida por `montarDataset`. */
  submetidaEm: Date
}

/**
 * Uma resolução pronta para desenhar. Só existe se a resolução TEM classe de tempo
 * (`ehPlotavel(tempoOrdem)`); ausências não viram ponto — viram `semMetrica` no rodapé.
 */
export interface PontoPlotavel extends PontoBase {
  /** Classe de TEMPO (0..7) = `k` do colormap. Eixo Y da Carta, cor de tudo. */
  k: ClasseK
  /** Classe de ESPAÇO (0..7) ou `null` quando não há dado / o motor não classificou. */
  kEspaco: ClasseK | null
  /** Ordem CRUA do espaço: `-1` = o motor não classificou (`?`) ≠ `null` = não há dado. */
  espacoOrdem: number | null
  /** Contagem de McCabe. NÃO é escala de colormap — só texto (callout: `M=4`). */
  ciclomatica: number | null
  /**
   * Confiança do MOTOR no valor que ele mesmo estimou (ALTA/MEDIA/BAIXA) — o eixo
   * SECUNDÁRIO. NÃO é MEDIDO×ESTIMADO: Big-O de tempo é sempre ≈ ESTIMADO
   * (`TIPO_METRICA_META.BIG_O_TEMPO.confianca`). Vira texto ("confiança do motor: alta"),
   * nunca preenche marcador.
   */
  confiancaTempo: NivelConfianca | null
  /** Rótulo canônico da classe de tempo: `O(n log n)`. Nunca nulo (deriva de `k`). */
  tempoRotulo: string
  /** Rótulo cru do espaço (`O(1)`, `?`) — `null` quando não há dado. */
  espacoRotulo: string | null
  visibilidade: Visibilidade
}

/**
 * A ORDEM CANÔNICA do portfólio: cronológica ASCENDENTE, empate resolvido pelo id (render
 * estável entre reloads). É o contrato de `dataset.pontos`/`dataset.todas`, da sequência das
 * constelações, dos irmãos de um cluster e do raio da Espiral. Uma função só, para que os três
 * módulos não desempatem de jeitos diferentes.
 */
export function porTempoAsc(a: PontoBase, b: PontoBase): number {
  const d = a.submetidaEm.getTime() - b.submetidaEm.getTime()
  return d !== 0 ? d : a.resolucaoId.localeCompare(b.resolucaoId)
}

/**
 * UM CLUSTER DA CARTA = todas as resoluções que caem na MESMA célula (mesma autonomia × mesma
 * classe). A Carta tem domínio DISCRETO (5 autonomias × 8 classes = 40 posições): duas resoluções
 * na mesma célula caem no mesmo pixel.
 *
 * ⚠ O jitter MORREU (era `deslocamentosCoincidentes`). Espalhar o grupo num anel de 5px produzia
 * marcadores grudados, impossíveis de acertar com o mouse, e dois callouts abrindo juntos. Um
 * grupo agora é UM marcador na posição EXATA da célula (`x`,`y` sem deslocamento nenhum), que
 * cresce com `total` e mostra o número no núcleo quando `total >= 2`. Nenhum dado some: o cluster
 * declara quantos são, e o painel lateral navega entre eles (`irmaosDoCluster`).
 */
export interface ClusterCarta {
  /** `` `${autonomia}:${k}` `` — a identidade da célula. Chave de React e de busca. */
  chave: string
  autonomia: NivelAutonomia
  k: ClasseK
  /** Posição EXATA da célula (`xDaAutonomia(autonomia)`). */
  x: number
  /** Posição EXATA da célula (`yDaClasse(k)`). */
  y: number
  /** Ordenados por `submetidaEm` ASC: `pontos[0]` é a mais ANTIGA (a que o clique abre). */
  pontos: PontoPlotavel[]
  /** = `pontos.length`. Sempre ≥ 1. */
  total: number
}

/**
 * Granularidade do eixo do tempo da Linha. É o MESMO vocabulário do enum do backend
 * (`GranularidadeTempo`, `types/api.ts`) — reaproveitado de propósito, para não existirem dois
 * nomes para a mesma coisa.
 *
 * ⚠ O cálculo, porém, é CLIENT-SIDE, a partir do `dataset` (`buckets()` em `escalas.ts`). NÃO
 * chamamos `/api/metricas/evolucao`: duas fontes de verdade sobre o mesmo dado exibem contagens
 * que não batem e não têm como ser explicadas ao aluno (§4.5 do índice — a mesma armadilha do
 * antigo `/resumo` × `/carta`).
 */
export type Granularidade = GranularidadeTempo

/** Ordem do seletor: do mais fino ao mais grosso. */
export const GRANULARIDADES: readonly Granularidade[] = ['DIARIO', 'SEMANAL', 'MENSAL']

/** Rótulo do seletor (pt-BR, minúsculo — é chip mono, não título). */
export const ROTULO_GRANULARIDADE: Record<Granularidade, string> = {
  DIARIO: 'diário',
  SEMANAL: 'semanal',
  MENSAL: 'mensal',
}

/** O portfólio de um aluno cresce em meses, não em dias: a visão de entrada é a mensal. */
export const GRANULARIDADE_PADRAO: Granularidade = 'MENSAL'

/**
 * Uma constelação = as resoluções do MESMO desafio, em ordem cronológica (§6-A, Lacuna 11).
 * É a trajetória que a pesquisa mede: força bruta + muito apoio de IA → refinado e autônomo.
 * Só existe com 2+ pontos plotáveis; desafio com 1 resolução é estrela solitária (sem linha).
 */
export interface Constelacao {
  desafioId: string
  desafioTitulo: string
  /** ≥ 2 pontos, ordenados por `submetidaEm` ascendente. */
  pontos: PontoPlotavel[]
}

/**
 * O DESCARTE, DECOMPOSTO. Um balde único ("5 sem métrica") colapsaria três estados
 * incompatíveis e coleria neles a explicação errada (§4.4: o descartado é contado E
 * explicado). Os três motivos, mutuamente exclusivos:
 */
export interface DescartesDataset {
  /** `calculando + semAnalisador + naoClassificado` — o que NÃO virou ponto. */
  total: number
  /** Java, mas a análise assíncrona ainda não terminou. Vai virar ponto sozinha. */
  calculando: number
  /** Linguagem sem analisador (hoje: tudo que não é Java) — é aqui que a nota só-Java vale. */
  semAnalisador: number
  /** O motor rodou e não classificou (`ordem === -1`), ou não produziu classe de tempo. */
  naoClassificado: number
}

/**
 * O dataset canônico dos 5 gráficos. `semMetrica` NUNCA é escondido: o rodapé do painel
 * exibe "18 de 23 resoluções plotadas · 2 calculando · 1 sem analisador" (`rotuloRodape`,
 * em `dataset.ts`). Esconder (ou explicar errado) o descarte seria desonesto — é o oposto
 * do que a pesquisa exige.
 */
export interface DatasetCarta {
  /** Resoluções plotáveis, em ordem cronológica ASCENDENTE (contrato das Órbitas). */
  pontos: PontoPlotavel[]
  /**
   * TODAS as resoluções recebidas (plotáveis ou não), em ordem cronológica ascendente.
   * A Linha lê daqui a série de AUTONOMIA — que independe da linguagem (§4.4).
   */
  todas: PontoBase[]
  /** O que ficou de fora dos gráficos, por motivo. */
  semMetrica: DescartesDataset
  /** Total recebido = `pontos.length + semMetrica.total` = `todas.length`. */
  total: number
  /** Trajetórias por desafio (≥ 2 resoluções plotáveis). */
  constelacoes: Constelacao[]
}

/**
 * Props comuns dos 5 gráficos. O tema é PROP (e não `useTheme()` interno) para que as
 * cores do colormap saiam de `corDaClasse(k, tema)` sem cada gráfico repetir o hook.
 */
export interface PropsGrafico {
  dataset: DatasetCarta
  /** `resolucaoId` do ponto selecionado (estrela cheia + cruzeta + painel lateral). */
  selecionadoId?: string | null
  /** Clique num ponto. Clique fora deve limpar a seleção (a página decide). */
  onSelecionar?: (resolucaoId: string) => void
  tema: Tema
}
