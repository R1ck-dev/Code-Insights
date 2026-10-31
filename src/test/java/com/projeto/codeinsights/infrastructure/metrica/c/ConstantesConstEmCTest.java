package com.projeto.codeinsights.infrastructure.metrica.c;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A sombra do {@code const int N} global: o nome sai do conjunto quando outra declaracao o redefine,
 * e fica quando so e lido em expressoes. O contexto decide, sem parser de declaracoes.
 */
class ConstantesConstEmCTest {

    private static final String CONSTANTE = "const int N = 100;\n";

    @Test
    @DisplayName("declarador por tipo colado ao nome tira o nome, inclusive tipo desconhecido (header)")
    void declaradorPorTipoColado() {
        assertThat(ConstantesConstEmC.colher(CONSTANTE + "void f(time_t N) { }")).doesNotContain("N");
        assertThat(ConstantesConstEmC.colher(CONSTANTE + "void f(void) { for (int N = 0; N < 3; N++) { } }"))
                .doesNotContain("N");
        assertThat(ConstantesConstEmC.colher(CONSTANTE + "void f(unsigned long N) { }")).doesNotContain("N");
    }

    @Test
    @DisplayName("declarador depois de virgula, em lista de declaracao ou de parametros")
    void declaradorEmLista() {
        assertThat(ConstantesConstEmC.colher(CONSTANTE + "void f(int a, int N) { }")).doesNotContain("N");
        assertThat(ConstantesConstEmC.colher(CONSTANTE + "void f(void) { int a = 1, *p, N; }")).doesNotContain("N");
        assertThat(ConstantesConstEmC.colher(CONSTANTE + "void f(myint a, N) { }")).doesNotContain("N");
    }

    @Test
    @DisplayName("leitura em expressao nao tira o nome: multiplicacao, return, argumento, indice, atribuicao")
    void leituraNaoTiraONome() {
        assertThat(ConstantesConstEmC.colher(CONSTANTE
                + "int f(int a) { int x = a * N; x = a * N; g(a, N); x = v[N]; if (a < N) return N; return x - N; }"))
                .contains("N");
        assertThat(ConstantesConstEmC.colher(CONSTANTE + "int f(int a) { int v[N], s = N, t = a + N; return s; }"))
                .contains("N");
    }

    @Test
    @DisplayName("volatile const nao e colhido, nem quando ha outro const com o mesmo nome")
    void volatileConstNaoEntra() {
        assertThat(ConstantesConstEmC.colher("volatile const int N = 5;")).doesNotContain("N");
        assertThat(ConstantesConstEmC.colher("static volatile const int N = 5;")).doesNotContain("N");
        assertThat(ConstantesConstEmC.colher("const int N = 5; void f(void) { volatile const int N = 7; }"))
                .doesNotContain("N");
    }

    @Test
    @DisplayName("dois const com literal de mesmo nome (escopos disjuntos) continuam colhidos")
    void doisConstComLiteral() {
        assertThat(ConstantesConstEmC.colher(CONSTANTE + "void f(void) { const int N = 7; }")).contains("N");
    }
}
