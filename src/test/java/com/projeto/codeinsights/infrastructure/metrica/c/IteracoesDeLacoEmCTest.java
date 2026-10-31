package com.projeto.codeinsights.infrastructure.metrica.c;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.infrastructure.metrica.custo.Custo;

/**
 * Quando um {@code for} em C nao depende da entrada. Os mesmos casos do {@code IteracoesDeLacoTest}
 * do lado Java: a regra e o contador do cabecalho - nasce constante no init, e comparado com limite
 * constante e avanca no passo - e nenhuma regra existe so numa linguagem. Os casos dividem-se nos que
 * exigem a constante (literal, {@code -1}, {@code 'a'}, init por atribuicao, limite a esquerda,
 * {@code #define}) e nos que impedem a regra de confundir "a condicao tem uma constante" com "o laco
 * e limitado" - flag de parada, contador que nao e o comparado, init que nao e constante.
 */
class IteracoesDeLacoEmCTest {

    /** O custo do primeiro {@code for} da funcao {@code f(int n, int trocou)}. */
    private static Custo primeiroFor(String cabecalho) {
        return primeiroFor("", cabecalho);
    }

    private static Custo primeiroFor(String declaracoesGlobais, String cabecalho) {
        String fonte = """
                %s
                void f(int n, int trocou) {
                    %s
                }
                """.formatted(declaracoesGlobais, cabecalho);
        NoDeC laco = ParserEstruturalDeC.analisar(fonte).funcoes().get(0).corpo().comDescendentes().stream()
                .filter(no -> no.tipo() == NoDeC.Tipo.FOR)
                .findFirst()
                .orElseThrow();
        return IteracoesDeLacoEmC.de(laco, ExpressoesDeC.constantesNomeadas(fonte)).custo();
    }

    @Test
    @DisplayName("contador declarado no cabecalho, limite literal: constante")
    void contadorDeclarado() {
        assertThat(primeiroFor("for (int i = 0; i < 26; i++) { }")).isEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("contador pre-declarado e iniciado por atribuicao: constante")
    void contadorPreDeclarado() {
        assertThat(primeiroFor("int i; for (i = 0; i < 26; i++) { }")).isEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("-1 a 1 e 'a' a 'z' sao constantes")
    void negativoEChar() {
        assertThat(primeiroFor("for (int di = -1; di <= 1; di++) { }")).isEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("for (char c = 'a'; c <= 'z'; c++) { }")).isEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("char c; for (c = 'a'; c <= 'z'; c++) { }")).isEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("limite a esquerda da comparacao (26 > i): constante")
    void limiteAEsquerda() {
        assertThat(primeiroFor("for (int i = 0; 26 > i; i++) { }")).isEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("limite vindo de #define, a diferenca legitima de C: constante")
    void limiteDeMacro() {
        assertThat(primeiroFor("#define MAX 100", "for (int i = 0; i < MAX; i++) { }")).isEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("#define MAX 100", "for (int i = 0; MAX > i; i++) { }")).isEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("flag de parada nao e o contador: nao e constante")
    void flagNaoEhConstante() {
        assertThat(primeiroFor("for (int passada = 0; trocou == 1; passada++) { trocou = 0; }"))
                .isNotEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("int p; for (p = 0; trocou == 1; p++) { trocou = 0; }"))
                .isNotEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("contador que nao e o comparado: for (d = 0; n > 0; d++) divide por 10 e e logaritmico")
    void contadorNaoComparado() {
        assertThat(primeiroFor("int d; for (d = 0; n > 0; d++) n /= 10;")).isEqualTo(Custo.LOGARITMICO);
    }

    @Test
    @DisplayName("init nao constante ou sem passo no cabecalho: nao e constante")
    void semInitConstanteOuSemPasso() {
        assertThat(primeiroFor("for (int i = n; i < 26; i++) { }")).isNotEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("for (int i = 0; i < 26; ) { i++; }")).isNotEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("limite que depende da entrada, ou condicao composta: nao e constante")
    void limiteVariavelOuComposto() {
        assertThat(primeiroFor("for (int i = 0; i < n; i++) { }")).isNotEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("for (int i = 0; i < 26 && trocou == 0; i++) { }")).isNotEqualTo(Custo.CONSTANTE);
    }

    /**
     * IC-11 (11a): {@code const int N = 100;} e o par de {@code static final int} do Java, colhido
     * no mesmo conjunto dos {@code #define}; o controle do {@code #define} com o mesmo formato e
     * {@code limiteDeMacro}, acima.
     */
    @Test
    @DisplayName("limite vindo de const int global, a constante nomeada de C: constante")
    void limiteDeConstGlobal() {
        assertThat(primeiroFor("const int N = 100;", "for (int i = 0; i < N; i++) { }")).isEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("const int N = 100;", "for (int i = 0; N > i; i++) { }")).isEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("limite vindo de const int local a funcao: constante")
    void limiteDeConstLocal() {
        assertThat(primeiroFor("const int N = 100; for (int i = 0; i < N; i++) { }")).isEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("controles do const: sem const, ou const sem literal, o limite continua sendo variavel")
    void constanteSoSeForConstEComLiteral() {
        assertThat(primeiroFor("int N = 100;", "for (int i = 0; i < N; i++) { }")).isNotEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("const int N = n; for (int i = 0; i < N; i++) { }")).isNotEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("int N = 100; for (int i = 0; i < N; i++) { }")).isNotEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("sombra: local N nao const (int ou outro tipo) redefinindo o const int N global tira o nome do conjunto")
    void sombraDoConstGlobalNaoEhConstante() {
        assertThat(primeiroFor("const int N = 100;", "int N = n; for (int i = 0; i < N; i++) { }"))
                .isNotEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("const int N = 100;", "double N = n; for (int i = 0; i < N; i++) { }"))
                .isNotEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("const comentado, qualificador depois do tipo e static const")
    void formasDeDeclararConst() {
        assertThat(primeiroFor("// const int N = 100;", "for (int i = 0; i < N; i++) { }"))
                .isNotEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("int const N = 100;", "for (int i = 0; i < N; i++) { }")).isEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("static const int N = 100;", "for (int i = 0; i < N; i++) { }"))
                .isEqualTo(Custo.CONSTANTE);
    }
}
