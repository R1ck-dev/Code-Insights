/*
 * MODELO DE DADOS PLOTÁVEL — o contrato dos 5 gráficos (Carta, Órbitas, Espectro,
 * Linha, Matriz). Todos leem do MESMO dataset (spec 02 §0.4): um ponto = uma resolução.
 *
 * `PontoPlotavel` é o `PontoCartaDTO` (types/api.ts) já RESOLVIDO para desenho:
 *   - `k` é uma classe real do colormap (0..7) — quem não tem classe de tempo não vira
 *     ponto (ver `dataset.ts`, regra de plotabilidade);
 *   - `confiancaTempo` já está no vocabulário do design (MEDIDO / ESTIMADO), não no do
 *     backend (ALTA / MEDIA / BAIXA);
 *   - `submetidaEm` já é `Date` (a ordem cronológica é dado: ângulo das Órbitas,
 *     sequência das constelações, buckets da Linha).
 *
 * ⚠ `k === 0` é O(1) — a MELHOR classe, não "vazio". Nunca `if (!k)`.
 */
import type { LinguagemProgramacao, Visibilidade } from '@/types/api'
import type { ClasseK, Confianca, Tema } from '@/domain/enums'

/** Índice de Autonomia IA — autodeclarado, 1 (mais apoio de IA) a 5 (mais autônomo). */
export type NivelAutonomia = 1 | 2 | 3 | 4 | 5

/** Menor e maior nível da escala de autonomia (eixo X da Carta, raio das Órbitas). */
export const AUTONOMIA_MIN = 1
export const AUTONOMIA_MAX = 5
/** 5 colunas: a escala de autonomia é discreta e completa. */
export const TOTAL_AUTONOMIA = 5

/**
 * Uma resolução pronta para desenhar. Só existe se a resolução TEM classe de tempo
 * (`ehPlotavel(tempoOrdem)`); ausências não viram ponto — viram `semMetrica` no rodapé.
 */
export interface PontoPlotavel {
  resolucaoId: string
  /** Agrupador das constelações: resoluções do mesmo desafio formam uma trajetória. */
  desafioId: string
  desafioTitulo: string
  linguagem: LinguagemProgramacao
  /** Eixo X da Carta · raio das Órbitas · colunas da Matriz. NEUTRA — nunca colormap. */
  autonomia: NivelAutonomia
  /** Classe de TEMPO (0..7) = `k` do colormap. Eixo Y da Carta, cor de tudo. */
  k: ClasseK
  /** Classe de ESPAÇO (0..7) ou `null` quando não há dado / o motor não classificou. */
  kEspaco: ClasseK | null
  /** Contagem de McCabe. NÃO é escala de colormap — só texto (callout: `M=4`). */
  ciclomatica: number | null
  /** MEDIDO (marcador cheio) vs ESTIMADO (marcador vazado + prefixo `≈`). Regra 3. */
  confiancaTempo: Confianca
  /** Rótulo canônico da classe de tempo: `O(n log n)`. Nunca nulo (deriva de `k`). */
  tempoRotulo: string
  /** Rótulo cru do espaço (`O(1)`, `?`) — `null` quando não há dado. */
  espacoRotulo: string | null
  visibilidade: Visibilidade
  /** Já parseada. Ordem cronológica ascendente é garantida por `montarDataset`. */
  submetidaEm: Date
}

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
 * O dataset canônico dos 5 gráficos. `semMetrica` NUNCA é escondido: o rodapé do painel
 * exibe "18 de 23 resoluções plotadas · 5 sem métrica" (`rotuloRodape`, em `dataset.ts`).
 * Esconder o descarte seria desonesto — é o oposto do que a pesquisa exige.
 */
export interface DatasetCarta {
  /** Resoluções plotáveis, em ordem cronológica ASCENDENTE (contrato das Órbitas). */
  pontos: PontoPlotavel[]
  /** Quantas resoluções o backend mandou e NÃO puderam ser plotadas (sem classe de tempo). */
  semMetrica: number
  /** Total recebido = `pontos.length + semMetrica`. */
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
