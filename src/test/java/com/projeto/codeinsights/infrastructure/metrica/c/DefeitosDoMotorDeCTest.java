package com.projeto.codeinsights.infrastructure.metrica.c;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;

/**
 * Defeitos que uma revisao adversarial do motor de C encontrou e que agora estao presos.
 * <p>
 * Cada teste guarda um <b>caso concreto</b> em que o motor respondia errado, e a maioria errava
 * para MENOS — dizer que a solucao custa menos do que custa e o erro perigoso numa ferramenta de
 * ensino. Estao juntos, e nao espalhados, porque contam a mesma historia: leitura de C por texto
 * erra em silencio, e so um caso que reproduz o erro impede que ele volte.
 */
class DefeitosDoMotorDeCTest {

    private final AnalisadorDeC analisador = new AnalisadorDeC(List.of(
            new BigOTempoDeCAnalisador(), new EspacoDeCAnalisador(), new CiclomaticaDeCAnalisador()));

    @Nested
    @DisplayName("parser")
    class Parser {

        /**
         * O pior deles: o rotulo caia no varredor de comando simples, que so para num {@code ;} de
         * nivel zero, e engolia como texto os lacos inteiros que vinham depois. Eles sumiam da
         * arvore e a solucao saia O(1) — o Big-O negava lacos que a ciclomatica, no mesmo arquivo,
         * contava.
         */
        @Test
        @DisplayName("rotulo de goto nao engole os lacos seguintes")
        void rotuloDeGotoNaoApagaOsLacos() {
            String comRotulo = """
                    int main(void) {
                        int i = 0;
                    repete:
                        while (i < n) {
                            for (int j = 0; j < n; j++) soma += m[i][j];
                            i++;
                        }
                        if (erro) goto repete;
                        return 0;
                    }
                    """;

            assertThat(tempo(comRotulo)).isEqualTo("O(n^2)");
        }

        @Test
        @DisplayName("rotulo antes de lacos aninhados tambem nao os apaga")
        void rotuloAntesDeLacoAninhado() {
            assertThat(tempo("""
                    int main(void) {
                    inicio:
                        for (int i = 0; i < n; i++)
                            for (int j = 0; j < n; j++)
                                soma += i * j;
                        return 0;
                    }
                    """)).isEqualTo("O(n^2)");
        }

        /**
         * A descida e recursiva, e {@code StackOverflowError} e um {@code Error}: passava por cima
         * do campo de falha e derrubava a analise inteira da resolucao, em vez de virar {@code "?"}
         * e deixar a ciclomatica sair.
         */
        @Test
        @DisplayName("aninhamento absurdo desiste em vez de estourar a pilha")
        void aninhamentoProfundoNaoDerrubaAAnalise() {
            int niveis = 400;
            String fonte = "int main(void) {" + "{ ".repeat(niveis) + "x++;" + "} ".repeat(niveis) + "}";

            List<ResultadoMetrica> resultados = analisar(fonte);

            assertThat(metrica(resultados, TipoMetrica.BIG_O_TEMPO).getRotulo()).isEqualTo("?");
            assertThat(metrica(resultados, TipoMetrica.COMPLEXIDADE_CICLOMATICA).getValor()).isPositive();
        }
    }

    @Nested
    @DisplayName("declaracoes")
    class Declaracoes {

        /** O corte no primeiro {@code =} da linha escondia todo declarador que viesse depois. */
        @Test
        @DisplayName("VLA declarada ao lado de um acumulador continua contando")
        void vlaDepoisDeInicializadorNaoSome() {
            assertThat(espaco("int main(void) { int total = 0, v[n]; return v[0] + total; }"))
                    .isEqualTo("O(n)");
        }

        /** Somar os colchetes da linha dobrava o grau: dois vetores viravam uma grade. */
        @Test
        @DisplayName("dois vetores na mesma linha sao O(n), e nao O(n^2)")
        void doisVetoresNaoViramMatriz() {
            assertThat(espaco("int main(void) { int a[n], b[n]; return a[0] + b[0]; }"))
                    .isEqualTo("O(n)");
        }

        @Test
        @DisplayName("uma matriz de verdade continua sendo O(n^2)")
        void matrizContinuaQuadratica() {
            assertThat(espaco("int main(void) { int m[n][n]; return m[0][0]; }")).isEqualTo("O(n^2)");
        }

        /** Trocar o tipo do vetor nao pode mudar a classe de espaco da solucao. */
        @Test
        @DisplayName("vetor de bool e de tipo com typedef tambem alocam")
        void tipoForaDaListaDeC89ContinuaSendoDeclaracao() {
            assertThat(espaco("int main(void) { bool visitado[n]; return visitado[0]; }"))
                    .isEqualTo("O(n)");
            assertThat(espaco("int main(void) { No fila[n]; return fila[0].valor; }"))
                    .isEqualTo("O(n)");
        }

        @Test
        @DisplayName("leitura indexada numa declaracao continua sem alocar")
        void leituraIndexadaNaoAloca() {
            assertThat(espaco("int main(void) { int soma = v[i] + v[j]; return soma; }"))
                    .isEqualTo("O(1)");
        }
    }

    @Nested
    @DisplayName("sizeof e macros")
    class SizeofEMacros {

        /** {@code sizeof(struct X)} e constante de compilacao tanto quanto {@code sizeof(int)}. */
        @Test
        @DisplayName("sizeof de tipo do aluno nao vira fator variavel")
        void sizeofDeTipoProprioEhConstante() {
            assertThat(espaco("""
                    #include <stdlib.h>
                    int main(void) {
                        struct Aluno *v = malloc(n * sizeof(struct Aluno));
                        return v[0].id;
                    }
                    """)).isEqualTo("O(n)");

            assertThat(espaco("""
                    #include <stdlib.h>
                    int main(void) { No *no = malloc(sizeof(No)); return no->valor; }
                    """)).isEqualTo("O(1)");
        }

        /** A regex roda sobre o fonte cru; exigir fim de linha apos o numero perdia a constante. */
        @Test
        @DisplayName("comentario ao lado do #define nao muda a classe")
        void comentarioNaoApagaAConstanteDeMacro() {
            assertThat(tempo("""
                    #define MAX 100  // tamanho maximo
                    int main(void) {
                        int v[MAX];
                        for (int i = 0; i < MAX; i++) v[i] = 0;
                        return 0;
                    }
                    """)).isEqualTo("O(1)");
        }
    }

    @Nested
    @DisplayName("lacos")
    class Lacos {

        /** {@code >>=} e {@code <<=} morriam junto com a regra criada para {@code <=} e {@code >=}. */
        @Test
        @DisplayName("deslocamento composto e reconhecido como progressao logaritmica")
        void deslocamentoCompostoEhLogaritmico() {
            assertThat(tempo("int main(void) { for (int i = n; i > 0; i >>= 1) total++; return 0; }"))
                    .isEqualTo("O(log n)");
        }

        /** Uma comparacao com literal em QUALQUER lugar da condicao declarava o laco constante. */
        @Test
        @DisplayName("condicao composta com && nao vira laco constante")
        void condicaoCompostaNaoEhConstante() {
            assertThat(tempo("""
                    int main(void) {
                        int achou = 0;
                        for (int i = 0; i < n && achou == 0; i++) if (v[i] == alvo) achou = 1;
                        return achou;
                    }
                    """)).isEqualTo("O(n)");
        }

        /** Procurar {@code *} como substring lia um passo aditivo como divisao. */
        @Test
        @DisplayName("passo aditivo que contem um produto nao vira logaritmico")
        void passoAditivoComProdutoNaoEhLogaritmico() {
            assertThat(tempo("""
                    int main(void) {
                        int resto = n, contador = 0;
                        while (resto > 0) { resto = resto - moeda * qtd; contador++; }
                        return contador;
                    }
                    """)).isEqualTo("O(n)");
        }

        /** Limite vindo de chamada nao se prova constante — a escolha segura e nao declarar. */
        @Test
        @DisplayName("limite dado por chamada de funcao nao vira laco constante")
        void limitePorChamadaNaoEhConstante() {
            assertThat(tempo("int main(void) { for (int i = 0; i < tamanho(); i++) soma += i; return 0; }"))
                    .isEqualTo("O(n)");
        }
    }

    @Nested
    @DisplayName("recursao")
    class Recursao {

        /**
         * O corpo do {@code switch} era lido como sequencia reta e a contagem parava no primeiro
         * {@code case} que retorna, descartando as auto-chamadas dos seguintes: recursao ramificada
         * virava recursao linear, e O(2^n) saia como O(n).
         */
        @Test
        @DisplayName("auto-chamadas em cases diferentes nao somem da contagem")
        void recursaoRamificadaPorSwitchNaoViraLinear() {
            assertThat(tempo("""
                    int conta(int n) {
                        if (n <= 1) return 1;
                        switch (n % 2) {
                            case 0: return conta(n - 1);
                            case 1: return conta(n - 1) + conta(n - 2);
                        }
                        return 0;
                    }
                    """)).isEqualTo("O(2^n)");
        }
    }

    @Nested
    @DisplayName("detalhe")
    class Detalhe {

        /** O aluno lia "chamada externa `int` desconhecida" — uma funcao que nao existe. */
        @Test
        @DisplayName("ponteiro de funcao nao vira uma chamada chamada int")
        void ponteiroDeFuncaoNaoViraChamada() {
            String detalhe = metrica(
                    analisar("int main(void) { int (*op)(int, int); op = soma; return op(1, 2); }"),
                    TipoMetrica.BIG_O_TEMPO).getDetalhe();

            assertThat(detalhe).doesNotContain("`int`");
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
