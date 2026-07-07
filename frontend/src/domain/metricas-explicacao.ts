/*
 * Texto explicativo (conceitual) de cada métrica do retrato, em termos técnicos
 * e leigos. Separado de TIPO_METRICA_META (que é só rótulo/natureza) para crescer
 * por adição — o `detalhe` que vem do backend continua sendo o raciocínio da
 * submissão específica; aqui mora a explicação do conceito.
 */
import type { TipoMetrica } from '@/types/api'

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
    subtitulo: 'Estimativa heurística a partir do AST',
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
    subtitulo: 'Estimativa heurística a partir do AST',
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
