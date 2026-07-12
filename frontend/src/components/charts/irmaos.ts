/*
 * IRMÃS DA SELEÇÃO — quem divide o marcador com a resolução selecionada.
 *
 * Os 3 gráficos AGRUPAM resoluções, e cada um agrupa por um critério DIFERENTE:
 *   · Carta  → mesma CÉLULA (autonomia × classe): 5 × 8 = 40 posições, várias resoluções caem
 *              no mesmo pixel e viram um marcador com o número no núcleo.
 *   · Matriz → a mesma célula da Carta, binada (é a Carta contada, não outro dado).
 *   · Linha  → mesmo BUCKET do eixo do tempo (mês, semana ou dia, conforme a granularidade).
 *
 * Em todos, o clique abre UMA resolução e as outras ficariam inalcançáveis — por isso o painel
 * lateral tem as setas "‹ 2 de 3 ›". O que estava errado (o usuário viu): as setas eram SEMPRE
 * as da célula da Carta, mesmo com a Linha na tela. Ou seja, o gráfico agrupava por período e o
 * painel navegava por autonomia × classe: a seta levava a uma resolução que não estava no ponto
 * clicado. Aqui o grupo passa a ser função da VISUALIZAÇÃO ATIVA — e o motivo é dito em voz alta,
 * porque uma seta que aparece sem explicar por que existe é pior do que não existir.
 */
import { CONFIANCA_BIG_O, comPrefixoEstimado, rotuloCanonico } from '@/domain/enums'
import { pluralPt } from '@/lib/utils'
import { buckets, irmaosDoCluster } from './escalas'
import type { TipoGrafico } from './SeletorDeGrafico'
import type { DatasetCarta, Granularidade, PontoPlotavel } from './tipos'

/**
 * O grupo em que a resolução selecionada está DENTRO do gráfico que está na tela.
 * `irmaos` inclui ela mesma, em ordem cronológica ASCENDENTE (a mais antiga primeiro) — a
 * mesma ordem em que o `Pagination` do sistema conta, e a única que o tempo autoriza.
 */
export interface GrupoDeIrmaos {
  /** Cabeçalho da faixa: o que agrupou estas resoluções. */
  titulo: string
  /** O PORQUÊ, em uma linha: "3 resoluções com autonomia 2 e ≈ O(n²)" · "2 resoluções em mai/2026". */
  motivo: string
  /** ≥ 1 ponto (a própria selecionada). Com 1 só, o painel não desenha navegador nenhum. */
  irmaos: PontoPlotavel[]
}

export interface ContextoDoGrupo {
  view: TipoGrafico
  /** Só importa na Linha — é ela que decide o tamanho do bucket. */
  granularidade: Granularidade
  dataset: DatasetCarta
  selecionadoId: string | null | undefined
}

/**
 * O grupo da resolução selecionada, do ponto de vista do gráfico ativo. `null` quando não há
 * seleção, quando a resolução não é plotável (não está em nenhum gráfico) ou quando o gráfico
 * não agrupa nada em torno dela.
 */
export function grupoDeIrmaos({
  view,
  granularidade,
  dataset,
  selecionadoId,
}: ContextoDoGrupo): GrupoDeIrmaos | null {
  if (!selecionadoId) return null
  const alvo = dataset.pontos.find((p) => p.resolucaoId === selecionadoId)
  if (!alvo) return null

  if (view === 'linha') return grupoDoPeriodo(dataset, granularidade, alvo)
  return grupoDaCelula(dataset, alvo, view)
}

/**
 * CARTA e MATRIZ: a célula (autonomia × classe). São a MESMA projeção — a Matriz é a Carta
 * contada —, então o grupo é o mesmo; só o nome do lugar muda, porque é assim que o usuário
 * chama o que está vendo.
 */
function grupoDaCelula(
  dataset: DatasetCarta,
  alvo: PontoPlotavel,
  view: TipoGrafico,
): GrupoDeIrmaos {
  const irmaos = irmaosDoCluster(dataset.pontos, alvo.resolucaoId)
  return {
    titulo: view === 'matriz' ? 'Mesma célula da matriz' : 'Mesma posição na carta',
    // Neutro de propósito: a classe já aparece em cor na linha de métrica logo abaixo, e repetir
    // o colormap aqui só acrescentaria ruído. O `≈` fica: a incerteza não some por ser texto
    // pequeno (regra 3).
    motivo:
      `${pluralPt(irmaos.length, 'resolução', 'resoluções')} com autonomia ${alvo.autonomia} ` +
      `e ${comPrefixoEstimado(rotuloCanonico(alvo.k), CONFIANCA_BIG_O)}`,
    irmaos,
  }
}

/**
 * LINHA: o bucket do eixo do tempo, na granularidade que está no seletor do gráfico. Só entram
 * as resoluções COM métrica — são as únicas que o painel sabe abrir (as sem classe não são
 * `PontoPlotavel`), e são exatamente as que o clique no bucket alcança.
 *
 * ⚠ Reagrega com o MESMO `buckets(...)` que a Linha desenha (não uma cópia da regra): trocar de
 * granularidade tem de mudar o grupo junto com o gráfico, ou a seta levaria a um período que não
 * está mais na tela.
 */
function grupoDoPeriodo(
  dataset: DatasetCarta,
  granularidade: Granularidade,
  alvo: PontoPlotavel,
): GrupoDeIrmaos | null {
  const janela = buckets(dataset.todas, granularidade)
  const bucket = janela.find((b) =>
    b.resolucoes.some((r) => r.resolucaoId === alvo.resolucaoId),
  )
  if (!bucket) return null

  const porId = new Map(dataset.pontos.map((p) => [p.resolucaoId, p]))
  const irmaos = bucket.comMetrica
    .map((r) => porId.get(r.resolucaoId))
    .filter((p): p is PontoPlotavel => !!p)

  return {
    titulo: 'Mesmo período',
    motivo: `${pluralPt(irmaos.length, 'resolução', 'resoluções')} em ${bucket.rotuloLongo}`,
    irmaos,
  }
}
