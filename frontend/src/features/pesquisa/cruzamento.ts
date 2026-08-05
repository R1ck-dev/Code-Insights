import { AUTONOMIA_MAX, AUTONOMIA_MIN } from '@/components/charts/tipos'
import type { NivelConfianca, PontoCartaDTO, ResolucaoDaCoorteDTO } from '@/types/api'

/*
 * A matemática da tela de cruzamento — sem React, para ser lida e conferida isolada.
 *
 * Existe separada de components/charts/ de propósito: o motor de gráficos é do portfólio de UM
 * aluno e não tem noção de participante. Cruzamento é conceito de pesquisa, e sujá-lo lá dentro
 * faria o dashboard do aluno carregar um vocabulário que não é dele.
 */

/**
 * A coorte não carrega `visibilidade` — a omissão é decisão registrada em `ResolucaoDaCoorteDTO`:
 * o que a pesquisa lê não é o portfólio público de ninguém. Mas `PontoCartaDTO` exige o campo,
 * então ele é **fabricado** aqui, e o valor escolhido é o mais restritivo.
 *
 * Isso é seguro porque a tela de cruzamento monta a Matriz SEM `onSelecionar`: nenhuma célula é
 * clicável, o `PainelEstrelaSelecionada` — único lugar que lê `visibilidade` — nunca é montado, e
 * portanto o valor fabricado não chega a afirmar nada na tela. Ligar seleção aqui quebraria essa
 * garantia e passaria a exibir um dado inventado.
 *
 * O `pseudonimo` fica DE FORA de propósito: quem conta participantes conta sobre a coorte crua,
 * não sobre o dataset do gráfico.
 */
export function paraPontoCarta(linha: ResolucaoDaCoorteDTO): PontoCartaDTO {
  return {
    resolucaoId: linha.resolucaoId,
    desafioId: linha.desafioId,
    desafioTitulo: linha.desafioTitulo,
    linguagem: linha.linguagem,
    indiceAutonomiaIA: linha.indiceAutonomiaIA,
    analisada: linha.analisada,
    tempoRotulo: linha.tempoRotulo,
    tempoOrdem: linha.tempoOrdem,
    confiancaTempo: linha.confiancaTempo,
    espacoRotulo: linha.espacoRotulo,
    espacoOrdem: linha.espacoOrdem,
    ciclomatica: linha.ciclomatica,
    visibilidade: 'PRIVADO',
    submetidaEm: linha.submetidaEm,
  }
}

/**
 * Abaixo deste número de resoluções, a coluna não vira proporção: vira os quadradinhos das
 * resoluções que existem.
 *
 * Percentual sobre n=3 produz segmentos de 33 pontos, e "33%" ao lado de "8%" lê-se como "4× mais"
 * mesmo com o `n=3` impresso do lado. Três unidades desenhadas leem-se como três unidades.
 */
export const PISO_PARA_PROPORCAO = 5

/**
 * Coluna com um participante só não é agregado, é a pessoa. A supressão é a regra padrão de
 * microdado: a contagem continua aparecendo, o detalhe individual não.
 */
export const MIN_PARTICIPANTES_PARA_DETALHE = 2

/** Uma resolução tem classe de tempo utilizável — o que a faz entrar na Matriz. */
export function temClasse(linha: ResolucaoDaCoorteDTO): boolean {
  return linha.tempoOrdem !== null && linha.tempoOrdem >= 0
}

export interface ColunaDeAutonomia {
  autonomia: number
  /** TODAS as resoluções deste nível, independentemente de linguagem ou de análise. */
  total: number
  /** Quantas delas têm classe de tempo — o denominador de qualquer leitura de complexidade. */
  comClasse: number
  participantes: number
  /** Contagem por classe 0..7, só entre as que têm classe. */
  porClasse: Map<number, number>
  /** As classes das resoluções, em ordem, para desenhar uma a uma abaixo do piso. */
  classes: number[]
  proporcaoConfiavel: boolean
  detalhavel: boolean
}

/**
 * Uma coluna por nível de autonomia, SEMPRE as cinco — inclusive as vazias.
 *
 * Os dois denominadores andam juntos e separados: `total` conta toda resolução daquele nível
 * (autonomia é autodeclarada e independe de linguagem), `comClasse` conta só as que o motor
 * conseguiu classificar. Num piloto em C, o primeiro é povoado e o segundo é zero — e é essa
 * distância que a tela precisa mostrar, porque uma coluna vazia sem explicação lê-se como
 * "ninguém submeteu" quando significa "não sabemos medir esta linguagem".
 */
export function montarColunas(coorte: ResolucaoDaCoorteDTO[]): ColunaDeAutonomia[] {
  const colunas: ColunaDeAutonomia[] = []

  for (let autonomia = AUTONOMIA_MIN; autonomia <= AUTONOMIA_MAX; autonomia++) {
    const doNivel = coorte.filter((l) => l.indiceAutonomiaIA === autonomia)
    const classificadas = doNivel.filter(temClasse)
    const porClasse = new Map<number, number>()
    for (const linha of classificadas) {
      const k = linha.tempoOrdem as number
      porClasse.set(k, (porClasse.get(k) ?? 0) + 1)
    }

    const participantes = new Set(doNivel.map((l) => l.pseudonimo)).size

    colunas.push({
      autonomia,
      total: doNivel.length,
      comClasse: classificadas.length,
      participantes,
      porClasse,
      classes: classificadas.map((l) => l.tempoOrdem as number).sort((a, b) => a - b),
      proporcaoConfiavel: classificadas.length >= PISO_PARA_PROPORCAO,
      detalhavel: participantes >= MIN_PARTICIPANTES_PARA_DETALHE,
    })
  }

  return colunas
}

export interface Recorte {
  resolucoes: number
  participantes: number
}

export function medirRecorte(coorte: ResolucaoDaCoorteDTO[]): Recorte {
  return {
    resolucoes: coorte.length,
    participantes: new Set(coorte.map((l) => l.pseudonimo)).size,
  }
}

export interface FiltrosDeSensibilidade {
  descartarConfiancaBaixa: boolean
  minimoDeResolucoesPorParticipante: number
}

export const SEM_FILTRO: FiltrosDeSensibilidade = {
  descartarConfiancaBaixa: false,
  minimoDeResolucoesPorParticipante: 1,
}

/**
 * O corte de confiança aceita `ALTA` **e** `MEDIA`.
 *
 * Antes ele era "só ALTA", e isso tornava o filtro inútil na pesquisa que ele deveria servir: C
 * nunca declara ALTA (o motor lê a estrutura por forma), então num piloto majoritariamente em C
 * ligar o corte esvaziava a amostra inteira — descartava a linguagem, não as medições ruins.
 *
 * Aceitar MEDIA não é afrouxar por conveniência: na faixa MEDIA o motor de C acerta 92,3% do Big O
 * de tempo, **acima** dos 85,7% do Java na mesma faixa (ver `acuracia-do-motor.md`). O que o corte
 * remove é `BAIXA` — onde o motor declara que não entendeu o código.
 */
const CONFIANCA_UTIL: NivelConfianca[] = ['ALTA', 'MEDIA']

/**
 * Os dois cortes de sensibilidade. Não "limpam" o dado — apertam a amostra, e a tela existe para
 * mostrar o preço disso lado a lado com o resultado.
 *
 * O corte por participante conta as resoluções que a pessoa tem **depois** do corte de confiança:
 * a ordem importa e é esta, senão o mínimo seria medido sobre linhas que já saíram.
 */
export function aplicarFiltros(
  coorte: ResolucaoDaCoorteDTO[],
  filtros: FiltrosDeSensibilidade,
): ResolucaoDaCoorteDTO[] {
  const porConfianca = filtros.descartarConfiancaBaixa
    ? coorte.filter((l) => l.confiancaTempo !== null && CONFIANCA_UTIL.includes(l.confiancaTempo))
    : coorte

  if (filtros.minimoDeResolucoesPorParticipante <= 1) return porConfianca

  const contagem = new Map<string, number>()
  for (const linha of porConfianca) {
    contagem.set(linha.pseudonimo, (contagem.get(linha.pseudonimo) ?? 0) + 1)
  }

  return porConfianca.filter(
    (l) => (contagem.get(l.pseudonimo) ?? 0) >= filtros.minimoDeResolucoesPorParticipante,
  )
}

/** O instante da análise mais recente do recorte — a safra do dado que está na tela. */
export function ultimaAnalise(coorte: ResolucaoDaCoorteDTO[]): string | null {
  return coorte.reduce<string | null>((maior, linha) => {
    if (!linha.analisadoEm) return maior
    return maior === null || linha.analisadoEm > maior ? linha.analisadoEm : maior
  }, null)
}
