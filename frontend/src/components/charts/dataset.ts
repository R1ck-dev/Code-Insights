/*
 * DATASET — a resposta da API virando algo plotável. É aqui que mora a honestidade
 * científica do painel: o que não pode ser medido não é inventado, e o que é descartado
 * é CONTADO e mostrado.
 *
 * Fontes: docs/design/specs/02-graficos.md §0.4 · 00-INDICE.md §4.4 e §6-A (Lacunas 2 e 11)
 *         frontend/src/types/api.ts (PontoCartaDTO — leia os comentários dele).
 */
import type { NivelConfianca, PontoCartaDTO } from '@/types/api'
import {
  type ClasseK,
  type Confianca,
  ehPlotavel,
  rotuloCanonico,
} from '@/domain/enums'
import { pluralPt } from '@/lib/utils'
import {
  AUTONOMIA_MAX,
  AUTONOMIA_MIN,
  type Constelacao,
  type DatasetCarta,
  type NivelAutonomia,
  type PontoPlotavel,
} from './tipos'

/** Dataset vazio — estado inicial / erro. Evita `dataset?.pontos` espalhado nas telas. */
export const DATASET_VAZIO: DatasetCarta = { pontos: [], semMetrica: 0, total: 0, constelacoes: [] }

/** Uma constelação precisa de pelo menos 2 pontos (§6-A, Lacuna 11). */
export const MIN_PONTOS_CONSTELACAO = 2

/**
 * Confiança do backend → vocabulário do design (00-INDICE §2.7, regra 3).
 * `ALTA` = MEDIDO · `MEDIA`/`BAIXA` = ≈ ESTIMADO.
 * `null` → ESTIMADO: na dúvida, NUNCA afirmar que a métrica foi medida.
 */
export function confiancaDeNivel(nivel: NivelConfianca | null | undefined): Confianca {
  return nivel === 'ALTA' ? 'MEDIDO' : 'ESTIMADO'
}

/**
 * REGRA DE PLOTABILIDADE (contrato §6-A, Lacuna 2). Um ponto só existe se tem classe de
 * TEMPO válida: `tempoOrdem` inteiro entre 0 e 7.
 *
 *   - `tempoOrdem === null` → não analisada, ou linguagem sem analisador (só Java tem).
 *   - `tempoOrdem === -1`   → DESCONHECIDO: o motor rodou e não conseguiu classificar.
 *
 * Os dois casos NÃO plotam — e nenhum deles some em silêncio (entram em `semMetrica`).
 * ⚠ `0` é O(1), uma complexidade legítima: jamais tratar 0 como ausência de dado.
 */
export function ehPontoPlotavel(dto: PontoCartaDTO): boolean {
  return ehPlotavel(dto.tempoOrdem)
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

/** Um DTO plotável → o ponto de desenho. Não chame direto: use `montarDataset`. */
function paraPonto(dto: PontoCartaDTO): PontoPlotavel {
  const k = dto.tempoOrdem as ClasseK // garantido por `ehPontoPlotavel`
  return {
    resolucaoId: dto.resolucaoId,
    desafioId: dto.desafioId,
    desafioTitulo: dto.desafioTitulo,
    linguagem: dto.linguagem,
    autonomia: nivelAutonomia(dto.indiceAutonomiaIA),
    k,
    // -1 (DESCONHECIDO) e null colapsam em null: nos dois casos não há classe para colorir.
    kEspaco: ehPlotavel(dto.espacoOrdem) ? (dto.espacoOrdem as ClasseK) : null,
    ciclomatica: dto.ciclomatica,
    confiancaTempo: confiancaDeNivel(dto.confiancaTempo),
    // O rótulo do backend é a fonte, mas `k` é a verdade: se vierem divergentes, vale `k`.
    tempoRotulo: rotuloCanonico(k),
    espacoRotulo: dto.espacoRotulo,
    visibilidade: dto.visibilidade,
    submetidaEm: parseData(dto.submetidaEm),
  }
}

/** Ordem cronológica ascendente; empate resolvido pelo id (render estável entre reloads). */
function porTempoAsc(a: PontoPlotavel, b: PontoPlotavel): number {
  const d = a.submetidaEm.getTime() - b.submetidaEm.getTime()
  return d !== 0 ? d : a.resolucaoId.localeCompare(b.resolucaoId)
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
 *   2. `pontos` está em ordem CRONOLÓGICA ASCENDENTE — as Órbitas usam o índice do array
 *      como `i` do ângulo (`θᵢ = -90° + i·(360°/n)`), sem reordenar nada.
 *   3. `semMetrica` = quantas resoluções foram descartadas. O rodapé do painel exibe esse
 *      número (`rotuloRodape`): o descarte é visível, nunca silencioso.
 *   4. `total` = tudo que o backend mandou.
 */
export function montarDataset(pontos: PontoCartaDTO[]): DatasetCarta {
  const total = pontos.length
  const plotaveis = pontos.filter(ehPontoPlotavel).map(paraPonto).sort(porTempoAsc)

  return {
    pontos: plotaveis,
    semMetrica: total - plotaveis.length,
    total,
    constelacoes: montarConstelacoes(plotaveis),
  }
}

/**
 * Rodapé do painel — a linha da honestidade (§4.4 do índice):
 *   "18 de 23 resoluções plotadas · 5 sem métrica"
 * Quando tudo plotou, o sufixo some ("23 de 23 resoluções plotadas").
 */
export function rotuloRodape(dataset: DatasetCarta): string {
  const { pontos, semMetrica, total } = dataset
  const base = `${pontos.length} de ${pluralPt(total, 'resolução plotada', 'resoluções plotadas')}`
  return semMetrica > 0 ? `${base} · ${semMetrica} sem métrica` : base
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
