/*
 * Texto explicativo (conceitual) de cada métrica do retrato, em termos técnicos
 * e leigos. Separado de TIPO_METRICA_META (que é só rótulo/natureza) para crescer
 * por adição — o `detalhe` que vem do backend continua sendo o raciocínio da
 * submissão específica; aqui mora a explicação do conceito.
 *
 * ── DUAS VOZES, DOIS LUGARES (decisão do usuário nesta rodada) ─────────────────
 * O motor grava, em cada análise, um `detalhe` AUDITÁVEL — "2 ponto(s) de decisao em 1
 * metodo(s)/construtor(es) (M = decisoes + P); metodo mais ramificado = 3." Ele é assim de
 * propósito: é o rastro que permite conferir, meses depois, POR QUE aquele número saiu — e a
 * pesquisa depende disso. Só que ele estava impresso no rodapé do card, como se fosse a leitura
 * do aluno. Não é: é a prova.
 *
 * Agora são duas vozes, cada uma no seu lugar:
 *   · no CARD  → `notaDaMetrica(...)`: uma frase em português corrente, derivada só do VALOR
 *                (nada de fórmula, nada de "(s)"). É o que o número quer dizer.
 *   · no `?`   → `secaoDoMotor(detalhe)`: o texto do motor, íntegro, sob "Como o motor chegou a
 *                este valor". Nada foi escondido — foi movido para onde é lido de propósito.
 *
 * Nenhuma análise precisou ser refeita: o backend continua gravando exatamente o que gravava.
 */
import { ORDEM_DESCONHECIDA, type ClasseK } from '@/domain/complexidade'
import type { ResultadoMetricaDTO, TipoMetrica } from '@/types/api'

export interface InfoSecao {
  rotulo: string
  texto: string
}

export interface MetricaExplicacao {
  titulo: string
  subtitulo: string
  secoes: InfoSecao[]
}

export const METRICA_EXPLICACAO: Record<TipoMetrica, MetricaExplicacao> = {
  COMPLEXIDADE_CICLOMATICA: {
    titulo: 'Complexidade ciclomática',
    subtitulo: 'McCabe · contagem exata no AST',
    secoes: [
      {
        rotulo: 'O que mede',
        texto:
          'O número de caminhos independentes que o fluxo do código pode seguir — na prática, quantos "e se…" (if, for, while, case, &&, ||, ?:, catch) existem na solução.',
      },
      {
        rotulo: 'Em termos técnicos',
        texto:
          'Métrica de McCabe: pontos de decisão + 1 por método. É contada diretamente na Árvore Sintática Abstrata (AST), sem executar o código — por isso é classificada como "exata".',
      },
      {
        rotulo: 'Em termos simples',
        texto:
          'Quanto maior, mais ramificações o código tem e mais casos são necessários para testá-lo por completo. Valores baixos indicam um fluxo linear, fácil de acompanhar.',
      },
      {
        rotulo: 'Por que importa aqui',
        texto:
          'É um indicador objetivo de esforço de teste e de legibilidade estrutural — ajuda a acompanhar se a solução do aluno vai ficando mais enxuta ou mais ramificada ao longo do tempo.',
      },
    ],
  },
  BIG_O_TEMPO: {
    titulo: 'Complexidade de tempo (Big O)',
    subtitulo: 'Estimativa por análise estática do AST',
    secoes: [
      {
        rotulo: 'O que mede',
        texto:
          'Como o tempo de execução cresce conforme a entrada aumenta — a classe Big O da solução (O(1), O(n), O(n²)…).',
      },
      {
        rotulo: 'Em termos técnicos',
        texto:
          'Inferida por análise estática do AST (aninhamento de laços, recursão, chamadas), não por execução. Não existe algoritmo que determine o Big O exato de um código qualquer (é indecidível no caso geral), então o valor é uma heurística — daí o rótulo "estimada".',
      },
      {
        rotulo: 'Em termos simples',
        texto:
          'Dá uma ideia de quão "cara" a solução fica com entradas grandes. Dois laços aninhados tendem a O(n²); um laço simples, O(n); uma busca que corta a entrada pela metade, O(log n).',
      },
      {
        rotulo: 'Por que importa aqui',
        texto:
          'Mapeia o amadurecimento algorítmico: sair da força bruta (ex.: O(n²)) para soluções mais refinadas (ex.: O(n) com uma estrutura auxiliar) é um dos sinais centrais do aprendizado prático.',
      },
    ],
  },
  COMPLEXIDADE_ESPACO: {
    titulo: 'Complexidade de espaço',
    subtitulo: 'Estimativa por análise estática do AST',
    secoes: [
      {
        rotulo: 'O que mede',
        texto:
          'Quanta memória adicional a solução usa conforme a entrada cresce — estruturas auxiliares (listas, mapas, matrizes) e a pilha de recursão.',
      },
      {
        rotulo: 'Em termos técnicos',
        texto:
          'Inferida do AST (alocações, coleções, profundidade de recursão), também por análise estática. Como o Big O de tempo, é uma heurística — "estimada".',
      },
      {
        rotulo: 'Em termos simples',
        texto:
          'Indica se a solução resolve "no lugar" (O(1) de espaço extra) ou se cria cópias/estruturas proporcionais à entrada (O(n) ou mais).',
      },
      {
        rotulo: 'Por que importa aqui',
        texto:
          'Revela o trade-off tempo × espaço nas escolhas do aluno: às vezes ganha-se tempo gastando memória (ex.: um HashMap), e acompanhar isso enriquece a leitura do amadurecimento.',
      },
    ],
  },
}

// ════════════════════════════════════════════════════════════════════════════
// A FRASE DO CARD — o que o número quer dizer, em português
// ════════════════════════════════════════════════════════════════════════════

/**
 * O que cada classe de TEMPO significa na prática — dita pelo efeito de DOBRAR a entrada, que é
 * a única forma de "sentir" um Big O sem falar de assíntota. Indexado por `k` (0..7).
 */
const EFEITO_TEMPO: Record<ClasseK, string> = {
  0: 'o tempo não muda com o tamanho da entrada',
  1: 'dobrar a entrada quase não muda o tempo — o custo cresce devagar',
  2: 'dobrar a entrada dobra o tempo',
  3: 'dobrar a entrada dobra o tempo e um pouco mais',
  4: 'dobrar a entrada multiplica o tempo por 4',
  5: 'dobrar a entrada multiplica o tempo por 8',
  6: 'cada item a mais DOBRA o tempo — só funciona em entradas pequenas',
  7: 'o tempo explode a cada item a mais — inviável fora de entradas mínimas',
}

/** O mesmo, para a MEMÓRIA extra que a solução aloca (estruturas auxiliares + pilha de recursão). */
const EFEITO_ESPACO: Record<ClasseK, string> = {
  0: 'usa memória fixa: não guarda nada que cresça com a entrada',
  1: 'a memória cresce muito devagar em relação à entrada',
  2: 'a memória cresce junto com a entrada — guarda uma estrutura do tamanho dela',
  3: 'a memória cresce um pouco mais rápido que a entrada',
  4: 'a memória cresce ao quadrado — algo como uma matriz n × n',
  5: 'a memória cresce ao cubo em relação à entrada',
  6: 'a memória dobra a cada item a mais',
  7: 'a memória explode a cada item a mais',
}

/** Como o valor de McCabe se lê: quanto maior, mais caminhos o teste precisa cobrir. */
function faixaCiclomatica(m: number): string {
  if (m <= 4) return 'fluxo simples, direto de acompanhar'
  if (m <= 7) return 'fluxo com algumas ramificações'
  if (m <= 10) return 'fluxo bem ramificado'
  return 'fluxo muito ramificado — vale considerar quebrar em partes'
}

/**
 * A frase do rodapé do card: o que aquele valor quer dizer, sem fórmula e sem jargão.
 *
 * ⚠ Ela é derivada do VALOR, e só. Nada aqui reinterpreta o `detalhe` do motor (que continua
 * íntegro no `?`, via `secaoDoMotor`) — parafrasear o rastro de auditoria seria criar uma segunda
 * versão dele, e duas versões da mesma prova é uma prova a menos.
 *
 * ⚠ REGRA 3 preservada: esta frase NÃO afirma medição. Ela descreve o comportamento da classe que
 * o motor ESTIMOU — o `≈` do valor, o marcador vazado e o chip de confiança continuam onde estão.
 */
export function notaDaMetrica(tipo: TipoMetrica, m: ResultadoMetricaDTO): string {
  if (tipo === 'COMPLEXIDADE_CICLOMATICA') {
    const caminhos = m.valor === 1 ? '1 caminho possível' : `${m.valor} caminhos possíveis`
    return `${caminhos} no código · ${faixaCiclomatica(m.valor)}.`
  }

  // O motor rodou e não classificou. É RESULTADO, não ausência — e dizê-lo é obrigatório (§4.4).
  if (m.valor === ORDEM_DESCONHECIDA || !(m.valor in EFEITO_TEMPO)) {
    return 'O motor analisou o código e não conseguiu classificar esta complexidade.'
  }

  const k = m.valor as ClasseK
  return tipo === 'BIG_O_TEMPO'
    ? `Na prática: ${EFEITO_TEMPO[k]}.`
    : `Na prática: ${EFEITO_ESPACO[k]}.`
}

/** Cabeçalho da seção do `?` que guarda o rastro do motor. Um nome só, usado nas duas telas. */
export const ROTULO_SECAO_MOTOR = 'Como o motor chegou a este valor'

/**
 * O `detalhe` do backend virando seção do `?` — ÍNTEGRO, sem paráfrase: é o rastro auditável da
 * análise (a pesquisa depende de poder conferi-lo). Devolve `[]` quando não há detalhe, para que
 * a chamada seja sempre um spread seguro: `secoes={[...base, ...secaoDoMotor(m?.detalhe)]}`.
 */
export function secaoDoMotor(detalhe: string | null | undefined): InfoSecao[] {
  if (!detalhe?.trim()) return []
  return [{ rotulo: ROTULO_SECAO_MOTOR, texto: detalhe }]
}
