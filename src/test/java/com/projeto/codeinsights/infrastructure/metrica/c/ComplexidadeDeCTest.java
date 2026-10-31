package com.projeto.codeinsights.infrastructure.metrica.c;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;

/**
 * Big-O de tempo e complexidade de espaco em C, pelo caminho que a producao usa.
 * <p>
 * O corpus mede acuracia sobre programas inteiros; aqui cada teste isola <b>uma regra</b>, para que
 * uma regressao aponte a regra que quebrou em vez de um numero agregado que caiu.
 */
class ComplexidadeDeCTest {

    private final AnalisadorDeC analisador = new AnalisadorDeC(List.of(
            new BigOTempoDeCAnalisador(), new EspacoDeCAnalisador(), new CiclomaticaDeCAnalisador()));

    @Nested
    @DisplayName("tempo")
    class Tempo {

        @Test
        @DisplayName("laco de passo unitario e linear; dois aninhados, quadratico")
        void aninhamentoMultiplica() {
            assertThat(tempo("""
                    int main(void) {
                        for (int i = 0; i < n; i++) total += i;
                    }
                    """)).isEqualTo("O(n)");

            assertThat(tempo("""
                    int main(void) {
                        for (int i = 0; i < n; i++)
                            for (int j = 0; j < n; j++)
                                total += i * j;
                    }
                    """)).isEqualTo("O(n^2)");
        }

        @Test
        @DisplayName("limite literal nao depende da entrada")
        void limiteLiteralEhConstante() {
            assertThat(tempo("""
                    int main(void) {
                        for (int i = 1; i <= 10; i++)
                            for (int j = 1; j <= 10; j++)
                                printf("%d", i * j);
                    }
                    """)).isEqualTo("O(1)");
        }

        /**
         * O jeito padrao de dimensionar vetor em C. Sem colher os {@code #define}, o {@code MAX}
         * seria um identificador desconhecido e toda solucao do piloto pareceria depender da entrada.
         */
        @Test
        @DisplayName("constante de #define e limite constante, e nao a entrada")
        void limitePorMacroEhConstante() {
            assertThat(tempo("""
                    #define MAX 100
                    int main(void) {
                        for (int i = 0; i < MAX; i++) v[i] = 0;
                    }
                    """)).isEqualTo("O(1)");
        }

        /**
         * Revisao da IC-11: a sombra do {@code const int N} global precisa enxergar a redeclaracao
         * em lista ({@code int i, N = n}), por parametro de tipo do aluno e o {@code volatile const}.
         * Em todas, {@code N} e a entrada: o laco e O(n), nunca O(1) com confianca.
         */
        @Test
        @DisplayName("sombra do const global por declaracao multipla (int i, N = n) e laco linear")
        void sombraPorDeclaracaoMultiplaNaoEhConstante() {
            assertThat(tempo("""
                    const int N = 100;
                    void f(int n) {
                        int i, N = n;
                        for (i = 0; i < N; i++) { }
                    }
                    """)).isEqualTo("O(n)");
        }

        @Test
        @DisplayName("sombra do const global por parametro de tipo do aluno (typedef) e laco linear")
        void sombraPorParametroDeTipoDoAlunoNaoEhConstante() {
            assertThat(tempo("""
                    typedef int myint;
                    const int N = 100;
                    void f(myint N) {
                        int i;
                        for (i = 0; i < N; i++) { }
                    }
                    """)).isEqualTo("O(n)");
        }

        @Test
        @DisplayName("sombra por parametro ponteiro de tipo do aluno e por struct (S *N) tambem tira o nome")
        void sombraPorPonteiroDeTipoDoAluno() {
            assertThat(ConstantesConstEmC.colher("""
                    typedef struct { int a; } Reg;
                    const int N = 100;
                    void f(Reg *N) { }
                    """)).doesNotContain("N");
            assertThat(ConstantesConstEmC.colher("""
                    struct S { int a; };
                    const int N = 100;
                    void f(struct S *N) { }
                    """)).doesNotContain("N");
        }

        @Test
        @DisplayName("volatile const nao e constante: o valor pode mudar fora do programa")
        void volatileConstNaoEhConstante() {
            assertThat(tempo("""
                    volatile const int N = 5;
                    void f(int n) {
                        int i;
                        for (i = 0; i < N; i++) { }
                    }
                    """)).isEqualTo("O(n)");
        }

        @Test
        @DisplayName("controle: const global lido em varias expressoes, sem redeclaracao, segue constante")
        void constLidoEmExpressoesSegueConstante() {
            assertThat(tempo("""
                    const int N = 100;
                    int g(int a, int b) { return a + b; }
                    void f(int n) {
                        int i, s = 0, v[N];
                        for (i = 0; i < N; i++) {
                            s += i * N;
                            s = g(s, N);
                            v[i] = (int) N;
                        }
                    }
                    """)).isEqualTo("O(1)");
        }

        @Test
        @DisplayName("indice que dobra e logaritmico")
        void indiceMultiplicativoEhLogaritmico() {
            assertThat(tempo("""
                    int main(void) {
                        for (int i = 1; i < n; i *= 2) total++;
                    }
                    """)).isEqualTo("O(log n)");
        }

        @Test
        @DisplayName("intervalo que encolhe pela metade e logaritmico, mesmo com passo aditivo")
        void intervaloQueEncolheEhLogaritmico() {
            assertThat(tempo("""
                    int busca(int v[], int n, int alvo) {
                        int lo = 0, hi = n - 1;
                        while (lo <= hi) {
                            int meio = (lo + hi) / 2;
                            if (v[meio] == alvo) return meio;
                            if (v[meio] < alvo) lo = meio + 1;
                            else hi = meio - 1;
                        }
                        return -1;
                    }
                    """)).isEqualTo("O(log n)");
        }

        /**
         * A armadilha mais comum de quem aprende C. O motor so a enxerga porque o custo da condicao
         * entra <b>dentro</b> do produto pelas iteracoes, e porque {@code strlen} esta tabelado.
         */
        @Test
        @DisplayName("strlen na condicao do laco e reavaliado a cada volta: O(n^2)")
        void chamadaNaCondicaoMultiplica() {
            assertThat(tempo("""
                    #include <string.h>
                    int main(void) {
                        char s[1000];
                        for (int i = 0; i < strlen(s); i++) total += s[i];
                    }
                    """)).isEqualTo("O(n^2)");
        }

        @Test
        @DisplayName("o mesmo laco com o strlen fora da condicao e linear")
        void chamadaForaDaCondicaoNaoMultiplica() {
            assertThat(tempo("""
                    #include <string.h>
                    int main(void) {
                        char s[1000];
                        int tamanho = strlen(s);
                        for (int i = 0; i < tamanho; i++) total += s[i];
                    }
                    """)).isEqualTo("O(n)");
        }

        @Test
        @DisplayName("qsort da biblioteca custa n log n")
        void bibliotecaPadraoEntraNaConta() {
            assertThat(tempo("""
                    #include <stdlib.h>
                    int main(void) {
                        qsort(v, n, sizeof(int), compara);
                        return 0;
                    }
                    """)).isEqualTo("O(n log n)");
        }

        @Test
        @DisplayName("chamada a funcao do arquivo dentro de laco multiplica o custo dela")
        void custoAtravessaAChamada() {
            assertThat(tempo("""
                    int soma(int v[], int n) {
                        int t = 0;
                        for (int i = 0; i < n; i++) t += v[i];
                        return t;
                    }

                    int main(void) {
                        for (int k = 0; k < n; k++) total += soma(v, n);
                        return 0;
                    }
                    """)).isEqualTo("O(n^2)");
        }

        @Test
        @DisplayName("funcao que ninguem chama nao contamina a estimativa")
        void codigoMortoNaoConta() {
            assertThat(tempo("""
                    int caro(int n) {
                        for (int i = 0; i < n; i++)
                            for (int j = 0; j < n; j++)
                                total++;
                        return total;
                    }

                    int main(void) { return 0; }
                    """)).isEqualTo("O(1)");
        }

        @Test
        @DisplayName("duas auto-chamadas em ramos exclusivos executam uma por caminho")
        void recursaoEmRamosExclusivosNaoEhExponencial() {
            assertThat(tempo("""
                    int busca(int v[], int lo, int hi, int alvo) {
                        if (lo > hi) return -1;
                        int meio = (lo + hi) / 2;
                        if (v[meio] == alvo) return meio;
                        if (v[meio] < alvo) return busca(v, meio + 1, hi, alvo);
                        return busca(v, lo, meio - 1, alvo);
                    }
                    """)).isEqualTo("O(log n)");
        }

        @Test
        @DisplayName("duas auto-chamadas no mesmo caminho, com n-1, sao exponenciais")
        void recursaoQueRamificaEhExponencial() {
            assertThat(tempo("""
                    int fib(int n) {
                        if (n < 2) return n;
                        return fib(n - 1) + fib(n - 2);
                    }
                    """)).isEqualTo("O(2^n)");
        }

        @Test
        @DisplayName("divide ao meio e faz trabalho linear: Teorema Mestre da n log n")
        void teoremaMestreVale() {
            assertThat(tempo("""
                    void ordena(int v[], int lo, int hi) {
                        if (lo >= hi) return;
                        int meio = (lo + hi) / 2;
                        ordena(v, lo, meio);
                        ordena(v, meio + 1, hi);
                        for (int i = lo; i <= hi; i++) v[i] = v[i];
                    }
                    """)).isEqualTo("O(n log n)");
        }

        @Test
        @DisplayName("estrutura ilegivel vira '?', e nao um numero inventado")
        void estruturaQuebradaNaoViraEstimativa() {
            assertThat(tempo("int main(void) { for (i = 0; i < n; i++) { total += i; }")).isEqualTo("?");
        }
    }

    @Nested
    @DisplayName("espaco")
    class Espaco {

        @Test
        @DisplayName("vetor de tamanho fixo nao cresce com a entrada")
        void vetorLiteralEhConstante() {
            assertThat(espaco("int main(void) { int v[1000]; return v[0]; }")).isEqualTo("O(1)");
        }

        @Test
        @DisplayName("#define tambem dimensiona vetor fixo")
        void vetorPorMacroEhConstante() {
            assertThat(espaco("""
                    #define MAX 1000
                    int main(void) { int v[MAX]; return v[0]; }
                    """)).isEqualTo("O(1)");
        }

        /** IC-11 (11a): {@code const int} com literal e colhido junto dos {@code #define}. */
        @Test
        @DisplayName("const int com literal tambem dimensiona vetor fixo")
        void vetorPorConstEhConstante() {
            assertThat(espaco("""
                    const int MAX = 1000;
                    int main(void) { int v[MAX]; return v[0]; }
                    """)).isEqualTo("O(1)");
        }

        @Test
        @DisplayName("controle: int sem const nao e constante, o vetor e de tamanho variavel")
        void vetorPorIntSemConstCresce() {
            assertThat(espaco("""
                    int MAX = 1000;
                    int main(void) { int v[MAX]; return v[0]; }
                    """)).isEqualTo("O(n)");
        }

        @Test
        @DisplayName("vetor de tamanho variavel cresce com a entrada")
        void vlaCresce() {
            assertThat(espaco("int main(void) { int v[n]; return v[0]; }")).isEqualTo("O(n)");
            assertThat(espaco("int main(void) { int m[n][n]; return m[0][0]; }")).isEqualTo("O(n^2)");
        }

        @Test
        @DisplayName("o grau do malloc vem dos fatores do tamanho, nao dos colchetes")
        void mallocContaFatores() {
            assertThat(espaco("""
                    #include <stdlib.h>
                    int main(void) { int *v = malloc(n * sizeof(int)); return v[0]; }
                    """)).isEqualTo("O(n)");

            assertThat(espaco("""
                    #include <stdlib.h>
                    int main(void) { char *g = malloc(n * n); return g[0]; }
                    """)).isEqualTo("O(n^2)");
        }

        /** Ler v[i] no inicializador nao reserva memoria nenhuma. */
        @Test
        @DisplayName("indexar vetor numa declaracao nao e alocacao")
        void leituraIndexadaNaoAloca() {
            assertThat(espaco("""
                    int main(void) {
                        int soma = v[i] + v[j];
                        return soma;
                    }
                    """)).isEqualTo("O(1)");
        }

        @Test
        @DisplayName("a pilha da divisao e conquista desce log n niveis, e nao n")
        void pilhaDeDivisaoEConquista() {
            assertThat(espaco("""
                    int busca(int v[], int lo, int hi, int alvo) {
                        if (lo > hi) return -1;
                        int meio = (lo + hi) / 2;
                        if (v[meio] == alvo) return meio;
                        if (v[meio] < alvo) return busca(v, meio + 1, hi, alvo);
                        return busca(v, lo, meio - 1, alvo);
                    }
                    """)).isEqualTo("O(log n)");
        }

        @Test
        @DisplayName("recursao de passo unitario guarda n quadros na pilha")
        void pilhaDeRecursaoLinear() {
            assertThat(espaco("""
                    int fatorial(int n) {
                        if (n <= 1) return 1;
                        return n * fatorial(n - 1);
                    }
                    """)).isEqualTo("O(n)");
        }
    }

    /**
     * IC-11b: funcoes que se chamam em ciclo, sem auto-chamada direta. Os testes de gabarito (linear,
     * exponencial, divisivo) afirmam a classe do ciclo fundido numa recorrencia so; os controles nao
     * podem mudar: sem reducao, ou com descida por laco, a resposta honesta e '?'.
     */
    @Nested
    @DisplayName("recursao mutua")
    class RecursaoMutua {

        private static final String PAR_IMPAR = """
                int ehImpar(int n);

                int ehPar(int n) {
                    if (n == 0) return 1;
                    return ehImpar(n - 1);
                }

                int ehImpar(int n) {
                    if (n == 0) return 0;
                    return ehPar(n - 1);
                }

                int main(void) { return ehPar(n); }
                """;

        @Test
        @DisplayName("par e impar mutuos, com n - 1, sao lineares")
        void parImparMutuoEhLinear() {
            assertThat(tempo(PAR_IMPAR)).isEqualTo("O(n)");
        }

        @Test
        @DisplayName("ciclo de tres funcoes, com n - 1, e linear")
        void trioMutuoEhLinear() {
            assertThat(tempo("""
                    int b(int n);
                    int c(int n);

                    int a(int n) {
                        if (n == 0) return 1;
                        return b(n - 1);
                    }

                    int b(int n) {
                        if (n == 0) return 0;
                        return c(n - 1);
                    }

                    int c(int n) {
                        if (n == 0) return 0;
                        return a(n - 1);
                    }

                    int main(void) { return a(n); }
                    """)).isEqualTo("O(n)");
        }

        /**
         * a(n) = 2 b(n-1) e b(n) = 2 a(n-1): a(n) = 4 a(n-2) = 2^n exato. Sem folga entre o
         * gabarito e a classe, e o caso que a fusao do ciclo (a = 2, reducao n-1) acerta em cheio.
         */
        @Test
        @DisplayName("mutua em que cada funcao chama a outra duas vezes, com n - 1, e O(2^n)")
        void exponencialMutuaSimetricaEhExponencial() {
            assertThat(tempo("""
                    int b(int n);

                    int a(int n) {
                        if (n <= 1) return 1;
                        return b(n - 1) + b(n - 1);
                    }

                    int b(int n) {
                        if (n <= 1) return 1;
                        return a(n - 1) + a(n - 1);
                    }

                    int main(void) { return a(n); }
                    """)).isEqualTo("O(2^n)");
        }

        /**
         * a(n) = 2 b(n-1) e b(n) = a(n-1): a(n) = 2 a(n-2), Theta(raiz de 2 elevada a n). Fundido
         * em a = 2 com n-1 sai O(2^n): superestima, o lado seguro; '?' ou classe abaixo seria erro
         * para menos. Mesmo contrato do controle Java (ic11b-exponencial-mutua).
         */
        @Test
        @DisplayName("mutua em que so uma das funcoes chama a outra duas vezes nao fica abaixo de O(2^n)")
        void exponencialMutuaAssimetricaNaoSubestima() {
            assertThat(tempo("""
                    int b(int n);

                    int a(int n) {
                        if (n <= 1) return 1;
                        return b(n - 1) + b(n - 1);
                    }

                    int b(int n) {
                        if (n <= 1) return 1;
                        return a(n - 1);
                    }

                    int main(void) { return a(n); }
                    """)).isEqualTo("O(2^n)");
        }

        /**
         * Cada funcao faz n de trabalho e chama a outra duas vezes com n/2: a(n) = 2 b(n/2) + n e
         * b(n) = 2 a(n/2) + n, logo a(n) = 4 a(n/4) + 2n. Teorema Mestre (a = b = 4, f = n), caso 2:
         * n log n. O ciclo fundido, 2 T(n/2) + n, da a mesma classe.
         */
        @Test
        @DisplayName("mutua divisiva, com n/2 e trabalho linear em cada funcao, e O(n log n)")
        void divisivoMutuoEhNLogN() {
            assertThat(tempo("""
                    long direita(int n);

                    long esquerda(int n) {
                        if (n <= 1) return 1;
                        long soma = 0;
                        for (int i = 0; i < n; i++) soma += i;
                        return soma + direita(n / 2) + direita(n / 2);
                    }

                    long direita(int n) {
                        if (n <= 1) return 1;
                        long soma = 0;
                        for (int i = 0; i < n; i++) soma += i;
                        return soma + esquerda(n / 2) + esquerda(n / 2);
                    }

                    int main(void) { return esquerda(n); }
                    """)).isEqualTo("O(n log n)");
        }

        /**
         * Revisao da IC-11: a(n) = 2 a(n-1) + b(n-1) e b(n) = a(n/2). Tomar a divisiva "que domina"
         * dava um polinomio para um real exponencial; entre membros nao ha reducao mista assumida.
         */
        @Test
        @DisplayName("controle: n-c num membro e n/b noutro nao tem recorrencia unica, e a resposta e '?'")
        void reducoesMistasEntreMembrosSaemIndeterminadas() {
            assertThat(tempo("""
                    int b(int n);

                    int a(int n) {
                        if (n <= 1) return 1;
                        return a(n - 1) + a(n - 1) + b(n - 1);
                    }

                    int b(int n) {
                        if (n <= 1) return 1;
                        return a(n / 2);
                    }

                    int main(void) { return a(n); }
                    """)).isEqualTo("?");
        }

        @Test
        @DisplayName("controle: a chama b(n/2) e c(n-1); membros de reducao diferente voltam '?'")
        void reducoesMistasNoTrioSaemIndeterminadas() {
            assertThat(tempo("""
                    int b(int n);
                    int c(int n);

                    int a(int n) {
                        if (n <= 1) return 1;
                        return b(n / 2) + c(n - 1);
                    }

                    int b(int n) {
                        if (n <= 1) return 1;
                        return a(n / 2);
                    }

                    int c(int n) {
                        if (n <= 1) return 1;
                        return a(n - 1);
                    }

                    int main(void) { return a(n); }
                    """)).isEqualTo("?");
        }

        @Test
        @DisplayName("controle: ciclo sem reducao nao tem recorrencia, e a resposta e '?'")
        void cicloSemReducaoContinuaIndeterminado() {
            assertThat(tempo("""
                    int b(int n);

                    int a(int n) {
                        if (n == 0) return 1;
                        return b(n);
                    }

                    int b(int n) {
                        if (n == 0) return 0;
                        return a(n);
                    }

                    int main(void) { return a(n); }
                    """)).isEqualTo("?");
        }

        @Test
        @DisplayName("controle: ciclo que desce por um laco nao e reducao aritmetica, e a resposta e '?'")
        void cicloPorLacoContinuaIndeterminado() {
            assertThat(tempo("""
                    int somaFilho(int i);

                    int somaNo(int n) {
                        int total = 0;
                        for (int i = 0; i < n; i++) total += somaFilho(i);
                        return total;
                    }

                    int somaFilho(int i) {
                        return somaNo(i);
                    }

                    int main(void) { return somaNo(n); }
                    """)).isEqualTo("?");
        }

        @Test
        @DisplayName("controle: o espaco do ciclo continua O(n), com ou sem resolver o tempo")
        void espacoDoCicloNaoMuda() {
            assertThat(espaco(PAR_IMPAR)).isEqualTo("O(n)");
        }
    }

    @Nested
    @DisplayName("teto de confianca")
    class TetoDeConfianca {

        /**
         * A garantia central da decisao de projeto: por mais limpo que seja o codigo, o motor de C
         * le a estrutura por forma, e forma nao e medicao. O filtro "somente ALTA" das telas de
         * pesquisa depende deste invariante para significar o que promete.
         */
        @Test
        @DisplayName("nenhuma metrica de C sai como ALTA, nem no codigo mais bem-comportado")
        void nenhumaMetricaDeCChegaAAlta() {
            List<ResultadoMetrica> resultados = analisar("""
                    int main(void) {
                        for (int i = 0; i < 10; i++) total += i;
                        return 0;
                    }
                    """);

            assertThat(resultados).isNotEmpty()
                    .allSatisfy(resultado -> assertThat(resultado.getConfianca()).isNotEqualTo(NivelConfianca.ALTA));
        }

        @Test
        @DisplayName("quando rebaixa, o detalhe diz por que")
        void oRebaixamentoEhExplicado() {
            String detalhe = metrica(analisar("int main(void) { return 0; }"), TipoMetrica.BIG_O_TEMPO).getDetalhe();

            assertThat(detalhe).contains("sem parse completo");
        }

        @Test
        @DisplayName("estrutura ilegivel continua BAIXA, e nao sobe para o teto")
        void oTetoNaoElevaConfianca() {
            List<ResultadoMetrica> resultados = analisar("int main(void) { for (;;) { ");

            assertThat(metrica(resultados, TipoMetrica.BIG_O_TEMPO).getConfianca())
                    .isEqualTo(NivelConfianca.BAIXA);
        }

        @Test
        @DisplayName("a ciclomatica sai mesmo quando o parser desiste do arquivo")
        void ciclomaticaSobreviveAEstruturaQuebrada() {
            List<ResultadoMetrica> resultados = analisar("int main(void) { if (a && b) { return 1;");

            assertThat(metrica(resultados, TipoMetrica.COMPLEXIDADE_CICLOMATICA).getValor()).isPositive();
        }
    }

    // ---------------------------------------------------------------- apoio

    private String tempo(String fonte) {
        return metrica(analisar(fonte), TipoMetrica.BIG_O_TEMPO).getRotulo();
    }

    private String espaco(String fonte) {
        return metrica(analisar(fonte), TipoMetrica.COMPLEXIDADE_ESPACO).getRotulo();
    }

    private List<ResultadoMetrica> analisar(String fonte) {
        return analisador.analisar(new Resolucao(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                fonte, LinguagemProgramacao.C, 1, null));
    }

    private static ResultadoMetrica metrica(List<ResultadoMetrica> resultados, TipoMetrica tipo) {
        return resultados.stream()
                .filter(resultado -> resultado.getTipo() == tipo)
                .findFirst()
                .orElseThrow(() -> new AssertionError("o motor nao produziu " + tipo));
    }
}
