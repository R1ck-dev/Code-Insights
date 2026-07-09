package com.projeto.codeinsights.infrastructure.metrica;

import java.util.List;

/**
 * Corpus de algoritmos canonicos com a complexidade <b>correta</b> anotada a mao
 * (gabarito da literatura, nao o que o motor calcula hoje). E o instrumento de
 * validacao do motor de metricas: {@code MotorDeMetricasCorpusTest} mede quanto
 * dele o motor acerta, e qualquer regressao aparece como um caso que falha.
 * <p>
 * Onde a resposta "certa" e ambigua (ex.: o espaco auxiliar de {@code Arrays.sort},
 * O(1) ou O(log n) conforme a implementacao), {@code espacoEsperado} e {@code null}
 * e o caso so valida o tempo.
 */
final class CorpusDeAlgoritmos {

    record Caso(String nome, String codigo, String tempoEsperado, String espacoEsperado) {

        @Override
        public String toString() {
            return nome;
        }
    }

    private CorpusDeAlgoritmos() {
    }

    static List<Caso> casos() {
        return List.of(
                new Caso("laco com limite constante", """
                        class Solucao {
                            int m() {
                                int s = 0;
                                for (int i = 0; i < 26; i++) s += i;
                                return s;
                            }
                        }
                        """, "O(1)", "O(1)"),

                new Caso("busca linear", """
                        class Solucao {
                            int indiceDe(int[] v, int alvo) {
                                for (int i = 0; i < v.length; i++) {
                                    if (v[i] == alvo) return i;
                                }
                                return -1;
                            }
                        }
                        """, "O(n)", "O(1)"),

                new Caso("laco de indice multiplicativo", """
                        class Solucao {
                            int m(int n) {
                                int c = 0;
                                for (int i = 1; i < n; i *= 2) c++;
                                return c;
                            }
                        }
                        """, "O(log n)", "O(1)"),

                new Caso("ordenacao bolha", """
                        class Solucao {
                            void ordena(int[] v) {
                                for (int i = 0; i < v.length - 1; i++) {
                                    for (int j = 0; j < v.length - 1 - i; j++) {
                                        if (v[j] > v[j + 1]) {
                                            int tmp = v[j];
                                            v[j] = v[j + 1];
                                            v[j + 1] = tmp;
                                        }
                                    }
                                }
                            }
                        }
                        """, "O(n^2)", "O(1)"),

                new Caso("tres lacos aninhados", """
                        class Solucao {
                            int m(int n) {
                                int s = 0;
                                for (int i = 0; i < n; i++)
                                    for (int j = 0; j < n; j++)
                                        for (int k = 0; k < n; k++) s++;
                                return s;
                            }
                        }
                        """, "O(n^3)", "O(1)"),

                new Caso("busca binaria iterativa", """
                        class Solucao {
                            int busca(int[] v, int alvo) {
                                int inicio = 0;
                                int fim = v.length - 1;
                                while (inicio <= fim) {
                                    int meio = inicio + (fim - inicio) / 2;
                                    if (v[meio] == alvo) return meio;
                                    if (v[meio] < alvo) inicio = meio + 1;
                                    else fim = meio - 1;
                                }
                                return -1;
                            }
                        }
                        """, "O(log n)", "O(1)"),

                new Caso("busca binaria recursiva", """
                        class Solucao {
                            int busca(int[] v, int alvo, int inicio, int fim) {
                                if (inicio > fim) return -1;
                                int meio = inicio + (fim - inicio) / 2;
                                if (v[meio] == alvo) return meio;
                                if (v[meio] < alvo) return busca(v, alvo, meio + 1, fim);
                                return busca(v, alvo, inicio, meio - 1);
                            }
                        }
                        """, "O(log n)", "O(log n)"),

                new Caso("merge sort", """
                        class Solucao {
                            void ordena(int[] v, int inicio, int fim) {
                                if (inicio >= fim) return;
                                int meio = (inicio + fim) / 2;
                                ordena(v, inicio, meio);
                                ordena(v, meio + 1, fim);
                                intercala(v, inicio, meio, fim);
                            }

                            private void intercala(int[] v, int inicio, int meio, int fim) {
                                int[] temporario = new int[fim - inicio + 1];
                                int i = inicio;
                                int j = meio + 1;
                                int k = 0;
                                while (i <= meio && j <= fim) {
                                    if (v[i] <= v[j]) temporario[k++] = v[i++];
                                    else temporario[k++] = v[j++];
                                }
                                while (i <= meio) temporario[k++] = v[i++];
                                while (j <= fim) temporario[k++] = v[j++];
                                for (int t = 0; t < temporario.length; t++) v[inicio + t] = temporario[t];
                            }
                        }
                        """, "O(n log n)", "O(n)"),

                new Caso("quick sort", """
                        class Solucao {
                            void ordena(int[] v, int inicio, int fim) {
                                if (inicio >= fim) return;
                                int pivo = particiona(v, inicio, fim);
                                ordena(v, inicio, pivo - 1);
                                ordena(v, pivo + 1, fim);
                            }

                            private int particiona(int[] v, int inicio, int fim) {
                                int referencia = v[fim];
                                int i = inicio - 1;
                                for (int j = inicio; j < fim; j++) {
                                    if (v[j] <= referencia) {
                                        i++;
                                        int tmp = v[i];
                                        v[i] = v[j];
                                        v[j] = tmp;
                                    }
                                }
                                int tmp = v[i + 1];
                                v[i + 1] = v[fim];
                                v[fim] = tmp;
                                return i + 1;
                            }
                        }
                        """, "O(n log n)", "O(log n)"),

                new Caso("fibonacci ingenuo", """
                        class Solucao {
                            long fib(int n) {
                                if (n < 2) return n;
                                return fib(n - 1) + fib(n - 2);
                            }
                        }
                        """, "O(2^n)", "O(n)"),

                new Caso("fibonacci memoizado", """
                        class Solucao {
                            private long[] memo = new long[64];

                            long fib(int n) {
                                if (n < 2) return n;
                                if (memo[n] != 0) return memo[n];
                                memo[n] = fib(n - 1) + fib(n - 2);
                                return memo[n];
                            }
                        }
                        """, "O(n)", "O(n)"),

                new Caso("fatorial recursivo", """
                        class Solucao {
                            long fatorial(int n) {
                                if (n <= 1) return 1;
                                return n * fatorial(n - 1);
                            }
                        }
                        """, "O(n)", "O(n)"),

                new Caso("exponenciacao rapida", """
                        class Solucao {
                            long potencia(long x, int n) {
                                if (n == 0) return 1;
                                long meio = potencia(x, n / 2);
                                if (n % 2 == 0) return meio * meio;
                                return x * meio * meio;
                            }
                        }
                        """, "O(log n)", "O(log n)"),

                new Caso("torre de hanoi", """
                        class Solucao {
                            void mover(int n, char origem, char destino, char auxiliar) {
                                if (n == 0) return;
                                mover(n - 1, origem, auxiliar, destino);
                                System.out.println(origem + " -> " + destino);
                                mover(n - 1, auxiliar, destino, origem);
                            }
                        }
                        """, "O(2^n)", "O(n)"),

                new Caso("duplicada por laco aninhado", """
                        class Solucao {
                            boolean temDuplicada(int[] v) {
                                for (int i = 0; i < v.length; i++)
                                    for (int j = i + 1; j < v.length; j++)
                                        if (v[i] == v[j]) return true;
                                return false;
                            }
                        }
                        """, "O(n^2)", "O(1)"),

                new Caso("duplicada com List.contains", """
                        import java.util.ArrayList;
                        import java.util.List;

                        class Solucao {
                            boolean temDuplicada(int[] v) {
                                List<Integer> vistos = new ArrayList<>();
                                for (int x : v) {
                                    if (vistos.contains(x)) return true;
                                    vistos.add(x);
                                }
                                return false;
                            }
                        }
                        """, "O(n^2)", "O(n)"),

                new Caso("duplicada com HashSet", """
                        import java.util.HashSet;
                        import java.util.Set;

                        class Solucao {
                            boolean temDuplicada(int[] v) {
                                Set<Integer> vistos = new HashSet<>();
                                for (int x : v) {
                                    if (vistos.contains(x)) return true;
                                    vistos.add(x);
                                }
                                return false;
                            }
                        }
                        """, "O(n)", "O(n)"),

                new Caso("ordenacao delegada a Arrays.sort", """
                        import java.util.Arrays;

                        class Solucao {
                            void ordena(int[] v) {
                                Arrays.sort(v);
                            }
                        }
                        """, "O(n log n)", null),

                new Caso("concatenacao de String em laco", """
                        class Solucao {
                            String repete(String s, int n) {
                                String r = "";
                                for (int i = 0; i < n; i++) r += s;
                                return r;
                            }
                        }
                        """, "O(n^2)", "O(n)"),

                new Caso("StringBuilder em laco", """
                        class Solucao {
                            String repete(String s, int n) {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < n; i++) sb.append(s);
                                return sb.toString();
                            }
                        }
                        """, "O(n)", "O(n)"),

                new Caso("TreeSet em laco", """
                        import java.util.Set;
                        import java.util.TreeSet;

                        class Solucao {
                            int distintos(int[] v) {
                                Set<Integer> ordenados = new TreeSet<>();
                                for (int x : v) ordenados.add(x);
                                return ordenados.size();
                            }
                        }
                        """, "O(n log n)", "O(n)"),

                new Caso("fila de prioridade em laco", """
                        import java.util.PriorityQueue;

                        class Solucao {
                            int menor(int[] v) {
                                PriorityQueue<Integer> fila = new PriorityQueue<>();
                                for (int x : v) fila.add(x);
                                return fila.poll();
                            }
                        }
                        """, "O(n log n)", "O(n)"),

                new Caso("programacao dinamica da mochila", """
                        class Solucao {
                            int melhor(int[] peso, int[] valor, int capacidade) {
                                int[][] dp = new int[peso.length + 1][capacidade + 1];
                                for (int i = 1; i <= peso.length; i++) {
                                    for (int c = 0; c <= capacidade; c++) {
                                        dp[i][c] = dp[i - 1][c];
                                        if (peso[i - 1] <= c) {
                                            dp[i][c] = Math.max(dp[i][c], dp[i - 1][c - peso[i - 1]] + valor[i - 1]);
                                        }
                                    }
                                }
                                return dp[peso.length][capacidade];
                            }
                        }
                        """, "O(n^2)", "O(n^2)"),

                new Caso("busca em profundidade com visitados", """
                        class Solucao {
                            void visita(int[][] adjacencia, boolean[] visitado, int atual) {
                                if (visitado[atual]) return;
                                visitado[atual] = true;
                                for (int proximo = 0; proximo < adjacencia.length; proximo++) {
                                    if (adjacencia[atual][proximo] == 1) {
                                        visita(adjacencia, visitado, proximo);
                                    }
                                }
                            }
                        }
                        """, "O(n^2)", "O(n)"),

                new Caso("lista de listas", """
                        import java.util.ArrayList;
                        import java.util.List;

                        class Solucao {
                            List<List<Integer>> grade(int n) {
                                List<List<Integer>> g = new ArrayList<>();
                                for (int i = 0; i < n; i++) {
                                    List<Integer> linha = new ArrayList<>();
                                    for (int j = 0; j < n; j++) linha.add(j);
                                    g.add(linha);
                                }
                                return g;
                            }
                        }
                        """, "O(n^2)", "O(n^2)"),

                new Caso("contagem de vogais com switch", """
                        class Solucao {
                            int vogais(String texto) {
                                int total = 0;
                                for (char c : texto.toCharArray()) {
                                    switch (c) {
                                        case 'a', 'e', 'i', 'o', 'u' -> total++;
                                        default -> { }
                                    }
                                }
                                return total;
                            }
                        }
                        """, "O(n)", "O(n)"),

                new Caso("metodo auxiliar chamado dentro de laco", """
                        class Solucao {
                            int soma(int[] v) {
                                int total = 0;
                                for (int x : v) total += x;
                                return total;
                            }

                            int somaRepetida(int[] v) {
                                int total = 0;
                                for (int i = 0; i < v.length; i++) total += soma(v);
                                return total;
                            }
                        }
                        """, "O(n^2)", "O(1)"),

                new Caso("metodo auxiliar morto nao contamina", """
                        class Solucao {
                            static void naoUsado(int n) {
                                for (int i = 0; i < n; i++)
                                    for (int j = 0; j < n; j++) {
                                        int x = i * j;
                                    }
                            }

                            static int usado(int[] v) {
                                int s = 0;
                                for (int x : v) s += x;
                                return s;
                            }

                            public static void main(String[] args) {
                                int[] v = new int[8];
                                System.out.println(usado(v));
                            }
                        }
                        """, "O(n)", "O(1)"),

                new Caso("recursao mutua e honestamente desconhecida", """
                        class Solucao {
                            boolean par(int n) {
                                return n == 0 ? true : impar(n - 1);
                            }

                            boolean impar(int n) {
                                return n == 0 ? false : par(n - 1);
                            }
                        }
                        """, "?", "O(n)"));
    }
}
