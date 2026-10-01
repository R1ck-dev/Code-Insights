package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;

/**
 * A chamada de metodo e ligada ao metodo da unidade pelo <b>tipo do receptor</b>.
 * <p>
 * Os casos dividem-se em dois: os que exigem que o metodo do aluno entre no custo ({@code ord.bolha(v)},
 * {@code esq.altura()}) e os que impedem que a regra invente uma ligacao que nao existe (um
 * {@code sort} do aluno nao e a {@code Arrays.sort}). Os segundos sao o preco da regra: resolver
 * pelo nome, sem olhar o tipo, passaria nos primeiros e quebraria estes.
 */
class ResolucaoDeChamadasTest {

    private final BigOTempoAnalisador tempo = new BigOTempoAnalisador();
    private final EspacoAnalisador espaco = new EspacoAnalisador();

    private String tempo(String codigo) {
        return tempo.analisar(AnalisadorTestSupport.parse(codigo)).rotulo();
    }

    private String espaco(String codigo) {
        return espaco.analisar(AnalisadorTestSupport.parse(codigo)).rotulo();
    }

    @Nested
    @DisplayName("o metodo do aluno entra no custo")
    class EntraNoCusto {

        private static final String DUAS_CLASSES_COM_O_MESMO_METODO = """
                class Pilha {
                    void insere(int[] v, int x) { v[0] = x; }
                }
                class Lista {
                    void insere(int[] v, int x) {
                        for (int i = 0; i < v.length; i++) if (v[i] == x) return;
                    }
                }
                """;

        /**
         * Duas classes com {@code insere(int[], int)}: a chave {@code nome/aridade} as confundia, e
         * a ultima declarada vencia. O receptor decide — e a resposta nao pode depender da ordem.
         */
        @Test
        @DisplayName("duas classes com o mesmo nome de metodo nao colidem: vale a do receptor")
        void valeAClasseDoReceptor() {
            String comLista = DUAS_CLASSES_COM_O_MESMO_METODO + """
                    class Main {
                        public static void main(String[] args) {
                            int[] v = new int[10];
                            Lista lista = new Lista();
                            for (int i = 0; i < v.length; i++) lista.insere(v, i);
                        }
                    }
                    """;
            String comPilha = DUAS_CLASSES_COM_O_MESMO_METODO + """
                    class Main {
                        public static void main(String[] args) {
                            int[] v = new int[10];
                            Pilha pilha = new Pilha();
                            for (int i = 0; i < v.length; i++) pilha.insere(v, i);
                        }
                    }
                    """;

            assertThat(tempo(comLista)).isEqualTo("O(n^2)");
            assertThat(tempo(comPilha)).isEqualTo("O(n)");
        }

        /** {@code var} nao diz o tipo, mas o {@code new} do inicializador diz. */
        @Test
        @DisplayName("var com new tem o tipo do new")
        void varComNewTemOTipoDoNew() {
            assertThat(tempo(DUAS_CLASSES_COM_O_MESMO_METODO + """
                    class Main {
                        public static void main(String[] args) {
                            int[] v = new int[10];
                            var lista = new Lista();
                            for (int i = 0; i < v.length; i++) lista.insere(v, i);
                        }
                    }
                    """)).isEqualTo("O(n^2)");
        }

        /** {@code g.vizinhos(u)} devolve {@code List<Integer>}; e a lista que decide o custo do {@code contains}. */
        @Test
        @DisplayName("receptor que e o resultado de um metodo da unidade tem o tipo de retorno dele")
        void receptorTipadoPeloRetornoDoMetodo() {
            assertThat(tempo("""
                    import java.util.*;

                    class Grafo {
                        List<List<Integer>> adj = new ArrayList<>();
                        List<Integer> vizinhos(int u) { return adj.get(u); }
                    }

                    class Main {
                        static int repetidas(Grafo g, int[][] arestas) {
                            int r = 0;
                            for (int[] e : arestas) if (g.vizinhos(e[0]).contains(e[1])) r++;
                            return r;
                        }
                    }
                    """)).isEqualTo("O(n^2)");
        }

        /**
         * Os dois campos tem nomes distintos de proposito: com o mesmo nome em duas classes, a tabela
         * de tipos plana deixa so um tipo, e a recursao some (caso aberto {@code D6} no manifesto).
         */
        @Test
        @DisplayName("recursao mutua por receptores entra na pilha")
        void recursaoMutuaPorReceptorEntraNaPilha() {
            assertThat(espaco("""
                    class Par {
                        Impar doImpar;
                        boolean ehPar(int n) { return n == 0 || doImpar.ehImpar(n - 1); }
                    }

                    class Impar {
                        Par doPar;
                        boolean ehImpar(int n) { return n != 0 && doPar.ehPar(n - 1); }
                    }
                    """)).isEqualTo("O(n)");
        }

        /**
         * Percorrer os filhos pelo receptor ({@code f.soma()}) e pelo argumento ({@code soma(f)})
         * e o mesmo algoritmo; so a escrita muda. As duas escritas precisam dar a mesma classe.
         * O valor absoluto vem do tratamento de laco sobre colecao, e nao deste teste.
         */
        @Test
        @DisplayName("percorrer os filhos pelo receptor ou pelo argumento da a mesma classe")
        void receptorEArgumentoDaoAMesmaClasse() {
            String porReceptor = """
                    import java.util.*;

                    class No {
                        List<No> filhos = new ArrayList<>();
                        int soma() {
                            int total = 1;
                            for (No f : filhos) total += f.soma();
                            return total;
                        }
                    }
                    """;
            String porArgumento = """
                    import java.util.*;

                    class No {
                        List<No> filhos = new ArrayList<>();
                    }

                    class Solucao {
                        static int soma(No no) {
                            int total = 1;
                            for (No f : no.filhos) total += soma(f);
                            return total;
                        }
                    }
                    """;

            assertThat(tempo(porReceptor)).isEqualTo(tempo(porArgumento));
        }
    }

    @Nested
    @DisplayName("a regra nao inventa uma ligacao que nao existe")
    class NaoInventaLigacao {

        /**
         * O {@code sort} do aluno so chama {@code Arrays.sort}. Resolvido pelo nome, ele viraria
         * auto-chamada: recursao inexistente, pilha {@code O(n)} e tempo "?".
         */
        @Test
        @DisplayName("sort do aluno que chama Arrays.sort nao e recursivo")
        void metodoDoAlunoComNomeDeBibliotecaNaoEhAutoChamada() {
            String codigo = """
                    import java.util.Arrays;

                    class Ordenacao {
                        static void sort(int[] v) { Arrays.sort(v); }
                    }
                    """;

            assertThat(tempo(codigo)).isEqualTo("O(n log n)");
            assertThat(espaco(codigo)).isEqualTo("O(1)");
        }

        /**
         * A classe do aluno tem um {@code contains} linear; a variavel e um {@code HashSet}. O tipo do
         * receptor manda a chamada para a tabela da biblioteca, onde {@code contains} e O(1).
         */
        @Test
        @DisplayName("receptor de tipo da biblioteca nao cai num metodo do aluno de mesmo nome")
        void receptorDaBibliotecaNaoCaiNoMetodoHomonimo() {
            assertThat(tempo("""
                    import java.util.*;

                    class Lista {
                        int[] dados = new int[10];
                        boolean contains(int x) {
                            for (int d : dados) if (d == x) return true;
                            return false;
                        }
                    }

                    class Main {
                        public static void main(String[] args) {
                            Set<Integer> vistos = new HashSet<>();
                            int[] v = new int[10];
                            for (int x : v) if (!vistos.contains(x)) vistos.add(x);
                        }
                    }
                    """)).isEqualTo("O(n)");
        }

        /**
         * A chamada polimorfica cai num metodo sem corpo. O custo das implementacoes nao e
         * considerado, e o motor nao pode responder isso com a confianca de quem mediu.
         */
        @Test
        @DisplayName("chamada a metodo abstrato nao e dada como medida")
        void metodoAbstratoNaoTemConfiancaAlta() {
            var resultado = tempo.analisar(AnalisadorTestSupport.parse("""
                    interface Forma { double area(); }

                    class Quadrado implements Forma {
                        double lado;
                        public double area() { return lado * lado; }
                    }

                    class Main {
                        public static void main(String[] args) {
                            Forma f = new Quadrado();
                            double a = f.area();
                        }
                    }
                    """));

            assertThat(resultado.confianca()).isEqualTo(NivelConfianca.MEDIA);
            assertThat(resultado.detalhe()).contains("metodo abstrato `area`");
        }
    }
}
