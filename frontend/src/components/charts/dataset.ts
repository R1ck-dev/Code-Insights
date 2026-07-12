/*
 * DATASET — a resposta da API virando algo plotável. É aqui que mora a honestidade
 * científica do painel: o que não pode ser medido não é inventado, e o que é descartado
 * é CONTADO e EXPLICADO.
 *
 * ⚠ NÃO EXISTE aqui nenhuma tradução de `NivelConfianca` (ALTA/MEDIA/BAIXA) para
 * MEDIDO/ESTIMADO. Os dois eixos são diferentes e confundi-los faz o gráfico afirmar que
 * uma estimativa foi medida:
 *   · MEDIDO × ≈ ESTIMADO = natureza do TIPO da métrica. Big-O de tempo/espaço é SEMPRE
 *     ≈ ESTIMADO (inferência estática; indecidível no caso geral). Ciclomática é MEDIDA.
 *     Fonte única: `TIPO_METRICA_META` (@/domain/enums).
 *   · ALTA/MEDIA/BAIXA = confiança do MOTOR no valor que ele próprio estimou. É informação
 *     secundária, exibida como TEXTO — nunca preenche um marcador.
 *
 * Fontes: docs/design/specs/02-graficos.md §0.4 · 00-INDICE.md §4.4 e §6-A (Lacunas 2 e 11)
 *         frontend/src/types/api.ts (PontoCartaDTO — leia os comentários dele).
 */
import type { PontoCartaDTO } from '@/types/api'
import {
  type ClasseK,
  LINGUAGEM_COM_METRICAS,
  ehPlotavel,
  rotuloCanonico,
} from '@/domain/enums'
import { pluralPt } from '@/lib/utils'
import {
  AUTONOMIA_MAX,
  AUTONOMIA_MIN,
  type Constelacao,
  type DatasetCarta,
  type DescartesDataset,
  type NivelAutonomia,
  type PontoBase,
  type PontoPlotavel,
  porTempoAsc,
} from './tipos'

/** Nenhum descarte. */
const SEM_DESCARTE: DescartesDataset = {
  total: 0,
  calculando: 0,
  semAnalisador: 0,
  naoClassificado: 0,
}

/** Dataset vazio — estado inicial / erro. Evita `dataset?.pontos` espalhado nas telas. */
export const DATASET_VAZIO: DatasetCarta = {
  pontos: [],
  todas: [],
  semMetrica: SEM_DESCARTE,
  total: 0,
  constelacoes: [],
}

/** Uma constelação precisa de pelo menos 2 pontos (§6-A, Lacuna 11). */
export const MIN_PONTOS_CONSTELACAO = 2

/**
 * REGRA DE PLOTABILIDADE (contrato §6-A, Lacuna 2). Um ponto só existe se tem classe de
 * TEMPO válida: `tempoOrdem` inteiro entre 0 e 7.
 *
 *   - `tempoOrdem === null` → não analisada, ou linguagem sem analisador (só Java tem).
 *   - `tempoOrdem === -1`   → DESCONHECIDO: o motor rodou e não conseguiu classificar.
 *
 * Os dois casos NÃO plotam — e nenhum deles some em silêncio (entram em `semMetrica`,
 * decomposto por motivo).
 * ⚠ `0` é O(1), uma complexidade legítima: jamais tratar 0 como ausência de dado.
 */
export function ehPontoPlotavel(dto: PontoCartaDTO): boolean {
  return ehPlotavel(dto.tempoOrdem)
}

/**
 * POR QUE esta resolução não virou ponto. A ordem dos testes é a ordem da causa:
 * a linguagem sem analisador nunca chega a ser analisada, então ela vem ANTES de
 * `!analisada` (senão uma resolução em Python ficaria eternamente "calculando").
 */
function motivoDoDescarte(dto: PontoCartaDTO): keyof Omit<DescartesDataset, 'total'> {
  if (dto.linguagem !== LINGUAGEM_COM_METRICAS) return 'semAnalisador'
  if (!dto.analisada) return 'calculando'
  return 'naoClassificado'
}

/** Autonomia sempre 1..5 (o backend valida; aqui é só um cinto de segurança). */
function nivelAutonomia(valor: number): NivelAutonomia {
  const n = Math.round(valor)
  if (!Number.isFinite(n)) return AUTONOMIA_MIN
  return Math.min(AUTONOMIA_MAX, Math.max(AUTONOMIA_MIN, n)) as NivelAutonomia
}

/** ISO → `Date`. Data ilegível vira epoch (ordena primeiro) em vez de envenenar a ordenação. */
function parseData(iso: string): Date {
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? new Date(0) : d
}

/** Todo DTO → o mínimo comum (autonomia, tempo, linguagem). Plotável ou não. */
function paraBase(dto: PontoCartaDTO): PontoBase {
  return {
    resolucaoId: dto.resolucaoId,
    desafioId: dto.desafioId,
    desafioTitulo: dto.desafioTitulo,
    linguagem: dto.linguagem,
    autonomia: nivelAutonomia(dto.indiceAutonomiaIA),
    analisada: dto.analisada,
    k: ehPlotavel(dto.tempoOrdem) ? (dto.tempoOrdem as ClasseK) : null,
    submetidaEm: parseData(dto.submetidaEm),
  }
}

/** Um DTO plotável → o ponto de desenho. Não chame direto: use `montarDataset`. */
function paraPonto(dto: PontoCartaDTO): PontoPlotavel {
  const k = dto.tempoOrdem as ClasseK // garantido por `ehPontoPlotavel`
  return {
    ...paraBase(dto),
    k,
    // `kEspaco` é a classe COLORÍVEL; `espacoOrdem` guarda o cru, porque -1 (o motor não
    // classificou → `?`) é semanticamente diferente de null (não há dado → `—`).
    kEspaco: ehPlotavel(dto.espacoOrdem) ? (dto.espacoOrdem as ClasseK) : null,
    espacoOrdem: dto.espacoOrdem,
    ciclomatica: dto.ciclomatica,
    // Confiança do MOTOR (ALTA/MEDIA/BAIXA) — não é MEDIDO/ESTIMADO. Vira texto.
    confiancaTempo: dto.confiancaTempo,
    // O rótulo do backend é a fonte, mas `k` é a verdade: se vierem divergentes, vale `k`.
    tempoRotulo: rotuloCanonico(k),
    espacoRotulo: dto.espacoRotulo,
    visibilidade: dto.visibilidade,
  }
}

/**
 * CONSTELAÇÕES (§6-A, Lacuna 11): agrupa os pontos PLOTÁVEIS por `desafioId`, ordena por
 * `submetidaEm` asc e mantém só os grupos com 2+ pontos. Cada constelação é a trajetória do
 * aluno naquele desafio — força bruta + IA → refinado + autônomo. É exatamente o dado que a
 * decisão D2 (nova resolução no lugar de editar) produz.
 *
 * Desafio com 1 resolução = estrela solitária, sem linha.
 * Recebe os pontos JÁ ordenados; a ordem das constelações segue a do 1º ponto de cada grupo.
 */
export function montarConstelacoes(pontosOrdenados: PontoPlotavel[]): Constelacao[] {
  const grupos = new Map<string, PontoPlotavel[]>()
  for (const p of pontosOrdenados) {
    const atual = grupos.get(p.desafioId)
    if (atual) atual.push(p)
    else grupos.set(p.desafioId, [p])
  }

  const constelacoes: Constelacao[] = []
  for (const [desafioId, pontos] of grupos) {
    if (pontos.length < MIN_PONTOS_CONSTELACAO) continue
    constelacoes.push({ desafioId, desafioTitulo: pontos[0].desafioTitulo, pontos })
  }
  return constelacoes
}

/**
 * A resposta da API → o dataset dos 5 gráficos.
 *
 * Garantias do contrato (os 5 gráficos dependem delas):
 *   1. `pontos` contém SÓ resoluções com classe de tempo válida (0..7).
 *   2. `pontos` e `todas` estão em ordem CRONOLÓGICA ASCENDENTE — a Espiral do tempo usa o
 *      índice do array como `i` do RAIO e do ângulo (`rᵢ` cresce com `i`: centro = mais antiga,
 *      borda = mais recente), sem reordenar nada.
 *   3. `todas` contém TUDO (inclusive o que não plota): a série de autonomia da Linha e a
 *      contagem mensal saem daqui. Autonomia é autodeclarada e independe da linguagem —
 *      um mês só com resoluções em Python NÃO é um mês "sem resolução".
 *   4. `semMetrica` diz quantas ficaram de fora E POR QUÊ. O rodapé do painel exibe isso
 *      (`rotuloRodape`): o descarte é visível e explicado, nunca silencioso.
 */
export function montarDataset(pontos: PontoCartaDTO[]): DatasetCarta {
  const total = pontos.length
  const todas = pontos.map(paraBase).sort(porTempoAsc)
  const plotaveis = pontos.filter(ehPontoPlotavel).map(paraPonto).sort(porTempoAsc)

  const semMetrica: DescartesDataset = { ...SEM_DESCARTE, total: total - plotaveis.length }
  for (const dto of pontos) {
    if (ehPontoPlotavel(dto)) continue
    semMetrica[motivoDoDescarte(dto)] += 1
  }

  return {
    pontos: plotaveis,
    todas,
    semMetrica,
    total,
    constelacoes: montarConstelacoes(plotaveis),
  }
}

/**
 * Rodapé do painel — a linha da honestidade (§4.4 do índice):
 *   "18 de 23 resoluções plotadas · 2 calculando · 3 sem analisador"
 * Quando tudo plotou, o sufixo some ("23 de 23 resoluções plotadas").
 *
 * O descarte NÃO é um balde só: dizer "5 sem métrica · métricas só para Java" quando as 5
 * são Java recém-submetidas é dar a explicação errada para um número certo.
 */
export function rotuloRodape(dataset: DatasetCarta): string {
  const { pontos, semMetrica, total } = dataset
  const base = `${pontos.length} de ${pluralPt(total, 'resolução plotada', 'resoluções plotadas')}`
  if (semMetrica.total === 0) return base

  const partes: string[] = []
  if (semMetrica.calculando > 0) partes.push(`${semMetrica.calculando} calculando`)
  if (semMetrica.semAnalisador > 0) partes.push(`${semMetrica.semAnalisador} sem analisador`)
  if (semMetrica.naoClassificado > 0) {
    partes.push(pluralPt(semMetrica.naoClassificado, 'não classificada', 'não classificadas'))
  }
  return `${base} · ${partes.join(' · ')}`
}

/** O ponto selecionado (painel "estrela selecionada"). `null` quando não há seleção. */
export function pontoPorId(
  dataset: DatasetCarta,
  resolucaoId: string | null | undefined,
): PontoPlotavel | null {
  if (!resolucaoId) return null
  return dataset.pontos.find((p) => p.resolucaoId === resolucaoId) ?? null
}

/** A constelação que contém um ponto — para destacá-la (opacidade .22 → .5) no hover/seleção. */
export function constelacaoDoPonto(
  dataset: DatasetCarta,
  resolucaoId: string | null | undefined,
): Constelacao | null {
  const ponto = pontoPorId(dataset, resolucaoId)
  if (!ponto) return null
  return dataset.constelacoes.find((c) => c.desafioId === ponto.desafioId) ?? null
}
