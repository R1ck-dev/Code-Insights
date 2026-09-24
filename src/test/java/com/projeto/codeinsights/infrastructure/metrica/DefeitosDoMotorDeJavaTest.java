package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;

/**
 * Defeitos que a revisao adversarial do motor de Java (05/08/2026) confirmou por leitura, agora
 * reproduzidos em execucao.
 * <p>
 * A revisao so deixou descricao de oito deles, de uma linha cada; o programa de cada teste e uma
 * reconstrucao concreta dessa descricao, e nao o caso original. Cada teste afirma a resposta
 * <b>correta</b> — o gabarito da literatura — e por isso so passa quando o defeito for corrigido.
 * A maioria erra para MENOS, que e a direcao perigosa: dizer ao aluno que a solucao custa menos do
 * que custa.
 * <p>
 * Onde da, o teste compara duas escritas do <b>mesmo</b> algoritmo: o defeito aparece como uma
 * forma de escrever que muda a classe, e o gabarito fica ancorado na escrita que o motor ja acerta.
 * <p>
 * Enquanto o defeito esta aberto, o teste leva {@code @Tag("defeito-aberto")} e fica fora do build
 * normal; {@code ./mvnw test -Pdefeitos-abertos} roda so esses. A correcao de cada um tira a tag do
 * seu teste, e ele passa a guardar a regressao como qualquer outro.
 */
class DefeitosDoMotorDeJavaTest {

    private final JavaParserAnalisadorMetricas analisador = new JavaParserAnalisadorMetricas(List.of(
            new BigOTempoAnalisador(), new EspacoAnalisador(), new CiclomaticaAnalisador()));

    private static final String ORDENADOR = """
            class Ordenador {
                void bolha(int[] v) {
                    for (int i = 0; i < v.length; i++)
                        for (int j = 0; j < v.length - 1 - i; j++)
                            if (v[j] > v[j + 1]) { int t = v[j]; v[j] = v[j + 1]; v[j + 1] = t; }
                }
            }
            """;

    @Nested
    @DisplayName("chamadas")
    class Chamadas {

        /**
         * So chamadas sem escopo ou com {@code this} eram tratadas como locais. {@code ord.bolha(v)}
         * caia em "chamada externa desconhecida, assumida O(1)" e o bubble sort sumia do custo.
         */
        @Test
        @Tag("defeito-aberto")
        @DisplayName("chamada qualificada a um metodo do proprio arquivo entra no custo")
        void chamadaQualificadaNaoViraConstante() {
            assertThat(tempo("""
                    import java.util.Scanner;

                    %s
                    public class Main {
                        public static void main(String[] args) {
                            Scanner sc = new Scanner(System.in);
                            int n = sc.nextInt();
                            int[] v = new int[n];
                            for (int i = 0; i < n; i++) v[i] = sc.nextInt();
                            Ordenador ord = new Ordenador();
                            ord.bolha(v);
                            System.out.println(v[0]);
                        }
                    }
                    """.formatted(ORDENADOR))).isEqualTo("O(n^2)");
        }

        @Test
        @Tag("defeito-aberto")
        @DisplayName("chamada estatica qualificada pelo nome da classe entra no custo")
        void chamadaEstaticaQualificadaNaoViraConstante() {
            assertThat(tempo("""
                    import java.util.Scanner;

                    class Ordenacao {
                        static void bolha(int[] v) {
                            for (int i = 0; i < v.length; i++)
                                for (int j = 0; j < v.length - 1 - i; j++)
                                    if (v[j] > v[j + 1]) { int t = v[j]; v[j] = v[j + 1]; v[j + 1] = t; }
                        }
                    }

                    public class Main {
                        public static void main(String[] args) {
                            Scanner sc = new Scanner(System.in);
                            int n = sc.nextInt();
                            int[] v = new int[n];
                            for (int i = 0; i < n; i++) v[i] = sc.nextInt();
                            Ordenacao.bolha(v);
                            System.out.println(v[0]);
                        }
                    }
                    """)).isEqualTo("O(n^2)");
        }

        /**
         * O agravante do defeito anterior: sem {@code main}, o motor parte das raizes do grafo de
         * chamadas e acha o bubble sort; com {@code main}, parte so dele e o perde. O mesmo
         * algoritmo mudava de classe conforme o aluno escreveu ou nao um {@code main}.
         */
        @Test
        @Tag("defeito-aberto")
        @DisplayName("ter ou nao um main nao muda a classe do mesmo algoritmo")
        void presencaDoMainNaoMudaAClasse() {
            String semMain = ORDENADOR;
            String comMain = ORDENADOR + """
                    class Main {
                        public static void main(String[] args) {
                            Ordenador ord = new Ordenador();
                            ord.bolha(new int[] {3, 1, 2});
                        }
                    }
                    """;

            assertThat(tempo(semMain)).isEqualTo("O(n^2)");
            assertThat(tempo(comMain)).isEqualTo(tempo(semMain));
        }

        /**
         * A chave de um metodo e {@code nome/aridade}: duas sobrecargas com a mesma aridade
         * colidem, e a ultima declarada vence. Aqui a vencedora e a O(1), e a chamada
         * {@code processa(v)} passava a custar O(1) — a resposta dependia da ordem das declaracoes.
         */
        @Test
        @Tag("defeito-aberto")
        @DisplayName("sobrecarga com a mesma aridade nao troca o metodo chamado")
        void sobrecargaNaoColide() {
            String quadratica = """
                        static int processa(int[] v) {
                            int pares = 0;
                            for (int i = 0; i < v.length; i++)
                                for (int j = i + 1; j < v.length; j++)
                                    if (v[i] + v[j] == 0) pares++;
                            return pares;
                        }
                    """;
            String constante = """
                        static int processa(int x) { return x * 2; }
                    """;
            String principal = """
                        public static void main(String[] args) {
                            Scanner sc = new Scanner(System.in);
                            int n = sc.nextInt();
                            int[] v = new int[n];
                            for (int i = 0; i < n; i++) v[i] = sc.nextInt();
                            System.out.println(processa(v));
                        }
                    """;
            String quadraticaPrimeiro = "import java.util.Scanner;\npublic class Main {\n"
                    + quadratica + constante + principal + "}\n";
            String constantePrimeiro = "import java.util.Scanner;\npublic class Main {\n"
                    + constante + quadratica + principal + "}\n";

            assertThat(tempo(constantePrimeiro)).isEqualTo("O(n^2)");
            assertThat(tempo(quadraticaPrimeiro)).isEqualTo("O(n^2)");
        }

        /**
         * Construtor nao e {@code MethodDeclaration}: nunca entrava no grafo de chamadas, e
         * {@code new Grafo(n)} custava O(1) mesmo com dois lacos aninhados dentro.
         */
        @Test
        @Tag("defeito-aberto")
        @DisplayName("o custo do construtor entra no custo de quem o chama")
        void construtorEntraNoCusto() {
            assertThat(tempo("""
                    import java.util.Scanner;

                    class Grafo {
                        int[][] adj;

                        Grafo(int n) {
                            adj = new int[n][n];
                            for (int i = 0; i < n; i++)
                                for (int j = 0; j < n; j++)
                                    adj[i][j] = (i + j) % 2;
                        }
                    }

                    public class Main {
                        public static void main(String[] args) {
                            Scanner sc = new Scanner(System.in);
                            int n = sc.nextInt();
                            Grafo g = new Grafo(n);
                            System.out.println(g.adj[0][0]);
                        }
                    }
                    """)).isEqualTo("O(n^2)");
        }
    }

    @Nested
    @DisplayName("lacos e streams")
    class LacosEStreams {

        /**
         * O lambda era avaliado como um filho qualquer da chamada, e o custo dele era SOMADO ao do
         * {@code filter}, em vez de multiplicado pelo numero de elementos. Ataca o sinal
         * pedagogico da pesquisa: trocar o laco por stream parecia melhora de classe sem mudar
         * nada no algoritmo.
         */
        @Test
        @Tag("defeito-aberto")
        @DisplayName("lambda de stream e multiplicado pelos elementos, como o corpo do laco")
        void lambdaEmStreamEhMultiplicado() {
            String comLaco = """
                    import java.util.List;

                    class Solucao {
                        static long emComum(List<Integer> a, List<Integer> b) {
                            long total = 0;
                            for (int x : a) if (b.contains(x)) total++;
                            return total;
                        }
                    }
                    """;
            String comStream = """
                    import java.util.List;

                    class Solucao {
                        static long emComum(List<Integer> a, List<Integer> b) {
                            return a.stream().filter(x -> b.contains(x)).count();
                        }
                    }
                    """;

            assertThat(tempo(comLaco)).isEqualTo("O(n^2)");
            assertThat(tempo(comStream)).isEqualTo("O(n^2)");
        }

        /**
         * Inicializador literal + comparacao com literal bastavam para declarar o laco constante —
         * e {@code trocou == 1} compara com literal. O laco externo do bubble sort com flag virava
         * O(1), e a ordenacao, O(n). Escrito com {@code while}, o mesmo laco sai certo.
         */
        @Test
        @Tag("defeito-aberto")
        @DisplayName("laco com flag de parada nao vira laco constante")
        void lacoComFlagNaoEhConstante() {
            String comWhile = """
                    class Solucao {
                        static void ordena(int[] v) {
                            int trocou = 1;
                            while (trocou == 1) {
                                trocou = 0;
                                for (int j = 0; j < v.length - 1; j++)
                                    if (v[j] > v[j + 1]) { int t = v[j]; v[j] = v[j + 1]; v[j + 1] = t; trocou = 1; }
                            }
                        }
                    }
                    """;
            String comFor = """
                    class Solucao {
                        static void ordena(int[] v) {
                            int trocou = 1;
                            for (int passada = 0; trocou == 1; passada++) {
                                trocou = 0;
                                for (int j = 0; j < v.length - 1; j++)
                                    if (v[j] > v[j + 1]) { int t = v[j]; v[j] = v[j + 1]; v[j + 1] = t; trocou = 1; }
                            }
                        }
                    }
                    """;

            assertThat(tempo(comWhile)).isEqualTo("O(n^2)");
            assertThat(tempo(comFor)).isEqualTo("O(n^2)");
        }
    }

    @Nested
    @DisplayName("tipos")
    class Tipos {

        /**
         * A tabela de tipos e um mapa plano do arquivo inteiro: a ULTIMA declaracao de um nome
         * vence em todos os metodos. Um segundo metodo com {@code Set<Integer> vistos} fazia o
         * {@code vistos.contains(x)} da lista, no primeiro, custar O(1) — o motor passava a ver a
         * troca de laco por tabela hash que o aluno nao fez.
         */
        @Test
        @Tag("defeito-aberto")
        @DisplayName("variavel de mesmo nome em outro metodo nao troca o tipo da primeira")
        void mesmoNomeEmOutroMetodoNaoTrocaOTipo() {
            String comLista = """
                        static int repetidosComLista(int[] v) {
                            List<Integer> vistos = new ArrayList<>();
                            int repetidos = 0;
                            for (int x : v) {
                                if (vistos.contains(x)) repetidos++;
                                else vistos.add(x);
                            }
                            return repetidos;
                        }
                    """;
            String comConjunto = """
                        static int repetidosComConjunto(int[] v) {
                            Set<Integer> vistos = new HashSet<>();
                            int repetidos = 0;
                            for (int x : v) if (!vistos.add(x)) repetidos++;
                            return repetidos;
                        }
                    """;
            String principal = """
                        public static void main(String[] args) {
                            Scanner sc = new Scanner(System.in);
                            int n = sc.nextInt();
                            int[] v = new int[n];
                            for (int i = 0; i < n; i++) v[i] = sc.nextInt();
                            System.out.println(repetidosComLista(v));
                        }
                    """;
            String cabecalho = "import java.util.*;\npublic class Main {\n";

            assertThat(tempo(cabecalho + comLista + principal + "}\n")).isEqualTo("O(n^2)");
            assertThat(tempo(cabecalho + comLista + comConjunto + principal + "}\n")).isEqualTo("O(n^2)");
        }
    }

    @Nested
    @DisplayName("recursao")
    class Recursao {

        /**
         * Argumento que vem de uma variavel local era lido como "pivo de particao" sempre que o
         * metodo tinha duas auto-chamadas e dois parametros numericos, e a recursao virava
         * {@code 2T(n/2)}. Guardar {@code lin - 1} numa variavel antes de chamar transformava
         * O(2^n) em O(n).
         */
        @Test
        @Tag("defeito-aberto")
        @DisplayName("n - 1 guardado numa variavel local nao vira pivo de particao")
        void localComSubtracaoNaoViraPivo() {
            String direto = """
                    class Solucao {
                        static long caminhos(int lin, int col) {
                            if (lin == 0 || col == 0) return 1;
                            return caminhos(lin - 1, col) + caminhos(lin, col - 1);
                        }
                    }
                    """;
            String comLocais = """
                    class Solucao {
                        static long caminhos(int lin, int col) {
                            if (lin == 0 || col == 0) return 1;
                            int acima = lin - 1;
                            int esquerda = col - 1;
                            return caminhos(acima, col) + caminhos(lin, esquerda);
                        }
                    }
                    """;

            assertThat(tempo(direto)).isEqualTo("O(2^n)");
            assertThat(tempo(comLocais)).isEqualTo("O(2^n)");
        }

        /**
         * Guarda que le o vetor + escrita no vetor bastavam para declarar memoizacao. Mas marcar na
         * ida e DESMARCAR na volta e backtracking, nao cache: o mesmo vertice e revisitado por
         * caminhos diferentes. Contar caminhos simples e exponencial no pior caso (fatorial no
         * grafo completo), e o motor dizia O(n^2), o custo da busca em profundidade comum.
         * <p>
         * O gabarito aceita as duas classes porque o defeito e a memoizacao falsa, e nao a escolha
         * entre exponencial e fatorial.
         */
        @Test
        @Tag("defeito-aberto")
        @DisplayName("marcar e desmarcar na volta e backtracking, e nao memoizacao")
        void desmarcarNaVoltaNaoEhMemoizacao() {
            String busca = """
                    class Solucao {
                        static int[][] adj;

                        static int caminhos(int u, int destino, boolean[] noCaminho) {
                            if (u == destino) return 1;
                            if (noCaminho[u]) return 0;
                            noCaminho[u] = true;
                            int total = 0;
                            for (int v = 0; v < adj.length; v++)
                                if (adj[u][v] == 1) total += caminhos(v, destino, noCaminho);
                            %s
                            return total;
                        }
                    }
                    """;

            assertThat(tempo(busca.formatted(""))).isEqualTo("O(n^2)");
            assertThat(tempo(busca.formatted("noCaminho[u] = false;"))).isIn("O(2^n)", "O(n!)");
        }
    }

    // ---------------------------------------------------------------- apoio

    private String tempo(String fonte) {
        return metrica(analisar(fonte), TipoMetrica.BIG_O_TEMPO).getRotulo();
    }

    private List<ResultadoMetrica> analisar(String fonte) {
        return analisador.analisar(new Resolucao(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                fonte, LinguagemProgramacao.JAVA, 1, null));
    }

    private static ResultadoMetrica metrica(List<ResultadoMetrica> resultados, TipoMetrica tipo) {
        return resultados.stream()
                .filter(resultado -> resultado.getTipo() == tipo)
                .findFirst()
                .orElseThrow(() -> new AssertionError("o motor nao produziu " + tipo));
    }
}
