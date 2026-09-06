// Biblioteca de snippets distribuida entre os alunos.
//
// Uma categoria por conceito do enum CategoriaConceito do dominio: se aparecer uma categoria nova
// la e nenhuma entrada aqui, o filtro por categoria fica com uma opcao vazia na tela.

export const SNIPPETS = [
    {
        categoria: 'ESTRUTURA_DADOS',
        descricao: 'Contagem de frequencia com HashMap — base de quase todo problema de agrupamento',
        codigo: `Map<Character, Integer> frequencia = new HashMap<>();
for (char letra : texto.toCharArray()) {
    frequencia.merge(letra, 1, Integer::sum);
}`,
    },
    {
        categoria: 'ESTRUTURA_DADOS',
        descricao: 'Fila de prioridade minima para os K menores elementos',
        codigo: `PriorityQueue<Integer> heap = new PriorityQueue<>(Comparator.reverseOrder());
for (int valor : valores) {
    heap.add(valor);
    if (heap.size() > k) {
        heap.poll();
    }
}`,
    },
    {
        categoria: 'RECURSAO',
        descricao: 'Memoizacao generica: transforma recursao exponencial em linear',
        codigo: `private static final Map<Integer, Long> CACHE = new HashMap<>();

static long termo(int n) {
    if (n <= 1) {
        return n;
    }
    return CACHE.computeIfAbsent(n, chave -> termo(chave - 1) + termo(chave - 2));
}`,
    },
    {
        categoria: 'RECURSAO',
        descricao: 'Backtracking: gerar todas as permutacoes de um vetor',
        codigo: `static void permutar(int[] vetor, int inicio, List<int[]> saida) {
    if (inicio == vetor.length) {
        saida.add(vetor.clone());
        return;
    }
    for (int i = inicio; i < vetor.length; i++) {
        trocar(vetor, inicio, i);
        permutar(vetor, inicio + 1, saida);
        trocar(vetor, inicio, i);
    }
}`,
    },
    {
        categoria: 'ORDENACAO',
        descricao: 'Particionamento do quicksort (esquema de Lomuto)',
        codigo: `static int particionar(int[] vetor, int inicio, int fim) {
    int pivo = vetor[fim];
    int limite = inicio - 1;
    for (int i = inicio; i < fim; i++) {
        if (vetor[i] <= pivo) {
            limite++;
            trocar(vetor, limite, i);
        }
    }
    trocar(vetor, limite + 1, fim);
    return limite + 1;
}`,
    },
    {
        categoria: 'ORDENACAO',
        descricao: 'Counting sort: O(n + k) quando o intervalo de valores e pequeno',
        codigo: `static void ordenarPorContagem(int[] vetor, int maiorValor) {
    int[] contagem = new int[maiorValor + 1];
    for (int valor : vetor) {
        contagem[valor]++;
    }
    int posicao = 0;
    for (int valor = 0; valor <= maiorValor; valor++) {
        while (contagem[valor] > 0) {
            vetor[posicao] = valor;
            posicao++;
            contagem[valor]--;
        }
    }
}`,
    },
    {
        categoria: 'GRAFOS',
        descricao: 'BFS em grade 4-direcional — esqueleto que serve para ilhas, labirinto e distancia',
        codigo: `int[] desviosLinha = { 1, -1, 0, 0 };
int[] desviosColuna = { 0, 0, 1, -1 };

Deque<int[]> fila = new ArrayDeque<>();
fila.add(new int[] { origemLinha, origemColuna });
while (!fila.isEmpty()) {
    int[] atual = fila.poll();
    for (int d = 0; d < 4; d++) {
        int linha = atual[0] + desviosLinha[d];
        int coluna = atual[1] + desviosColuna[d];
        if (dentroDaGrade(linha, coluna) && !visitado[linha][coluna]) {
            visitado[linha][coluna] = true;
            fila.add(new int[] { linha, coluna });
        }
    }
}`,
    },
    {
        categoria: 'GRAFOS',
        descricao: 'Union-Find com compressao de caminho',
        codigo: `int encontrar(int no) {
    if (pai[no] != no) {
        pai[no] = encontrar(pai[no]);
    }
    return pai[no];
}

void unir(int a, int b) {
    pai[encontrar(a)] = encontrar(b);
}`,
    },
    {
        categoria: 'PROGRAMACAO_DINAMICA',
        descricao: 'Mochila 0/1 com vetor unico — o laco interno decresce para nao reusar o item',
        codigo: `int[] melhor = new int[capacidade + 1];
for (int i = 0; i < pesos.length; i++) {
    for (int c = capacidade; c >= pesos[i]; c--) {
        melhor[c] = Math.max(melhor[c], melhor[c - pesos[i]] + valores[i]);
    }
}`,
    },
    {
        categoria: 'PROGRAMACAO_DINAMICA',
        descricao: 'Maior subsequencia comum entre duas cadeias',
        codigo: `int[][] tabela = new int[a.length() + 1][b.length() + 1];
for (int i = 1; i <= a.length(); i++) {
    for (int j = 1; j <= b.length(); j++) {
        if (a.charAt(i - 1) == b.charAt(j - 1)) {
            tabela[i][j] = tabela[i - 1][j - 1] + 1;
        } else {
            tabela[i][j] = Math.max(tabela[i - 1][j], tabela[i][j - 1]);
        }
    }
}`,
    },
    {
        categoria: 'STRINGS',
        descricao: 'Janela deslizante: maior substring sem caractere repetido',
        codigo: `Map<Character, Integer> ultimaPosicao = new HashMap<>();
int inicio = 0;
int melhor = 0;
for (int fim = 0; fim < texto.length(); fim++) {
    char atual = texto.charAt(fim);
    if (ultimaPosicao.containsKey(atual)) {
        inicio = Math.max(inicio, ultimaPosicao.get(atual) + 1);
    }
    ultimaPosicao.put(atual, fim);
    melhor = Math.max(melhor, fim - inicio + 1);
}`,
    },
    {
        categoria: 'STRINGS',
        descricao: 'Normalizar texto para comparacao (remove acento, espaco e caixa)',
        codigo: `static String normalizar(String texto) {
    return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\\\p{M}", "")
            .replaceAll("\\\\s+", "")
            .toLowerCase();
}`,
    },
    {
        categoria: 'MATEMATICA',
        descricao: 'MDC por Euclides, e o MMC que sai dele',
        codigo: `static long mdc(long a, long b) {
    return b == 0 ? a : mdc(b, a % b);
}

static long mmc(long a, long b) {
    return a / mdc(a, b) * b;
}`,
    },
    {
        categoria: 'MATEMATICA',
        descricao: 'Exponenciacao modular rapida — O(log expoente)',
        codigo: `static long potenciaModular(long base, long expoente, long modulo) {
    long resultado = 1;
    base = base % modulo;
    while (expoente > 0) {
        if ((expoente & 1) == 1) {
            resultado = resultado * base % modulo;
        }
        base = base * base % modulo;
        expoente >>= 1;
    }
    return resultado;
}`,
    },
];
