package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BigOTempoAnalisadorTest {

    private final BigOTempoAnalisador analisador = new BigOTempoAnalisador();

    private String bigO(String codigo) {
        return analisador.analisar(AnalisadorTestSupport.parse(codigo)).rotulo();
    }

    @Test
    void semLacoNemRecursaoEhConstante() {
        assertThat(bigO("class A { void m() { int x = 1; } }")).isEqualTo("O(1)");
    }

    @Test
    void umLacoLinearEhLinear() {
        assertThat(bigO("class A { void m(int n) { int s = 0; for (int i = 0; i < n; i++) { s += i; } } }"))
                .isEqualTo("O(n)");
    }

    @Test
    void doisLacosAninhadosEhQuadratico() {
        String codigo = "class A { void m(int n) { for (int i = 0; i < n; i++) { for (int j = 0; j < n; j++) {} } } }";
        assertThat(bigO(codigo)).isEqualTo("O(n^2)");
    }

    @Test
    void tresLacosAninhadosEhCubico() {
        String codigo = "class A { void m(int n) { for (int i=0;i<n;i++){ for (int j=0;j<n;j++){ for (int k=0;k<n;k++){} } } } }";
        assertThat(bigO(codigo)).isEqualTo("O(n^3)");
    }

    @Test
    void lacoComIndiceMultiplicativoEhLogaritmico() {
        assertThat(bigO("class A { void m(int n) { for (int i = 1; i < n; i *= 2) {} } }"))
                .isEqualTo("O(log n)");
    }

    @Test
    void lacoLinearComLacoLogaritmicoInternoEhNLogN() {
        String codigo = "class A { void m(int n) { for (int i=0;i<n;i++){ for (int j=1;j<n;j*=2){} } } }";
        assertThat(bigO(codigo)).isEqualTo("O(n log n)");
    }

    @Test
    void recursaoLinearEhLinear() {
        assertThat(bigO("class A { int f(int n) { if (n == 0) return 0; return f(n - 1); } }"))
                .isEqualTo("O(n)");
    }

    @Test
    void recursaoComDuasChamadasEhExponencial() {
        assertThat(bigO("class A { int fib(int n) { if (n < 2) return n; return fib(n-1) + fib(n-2); } }"))
                .isEqualTo("O(2^n)");
    }

    @Test
    void lacosIrmaosNaoAninhadosNaoInflamAClasse() {
        String codigo = "class A { void m(int n) { for (int i=1;i<n;i*=2){} for (int j=0;j<n;j++){} } }";
        assertThat(bigO(codigo)).isEqualTo("O(n)");
    }

    @Test
    void lacoLogaritmicoSoltoNaoRebaixaLacosAninhados() {
        String codigo = "class A { void m(int n) { for (int i=1;i<n;i*=2){} "
                + "for (int a=0;a<n;a++){ for (int b=0;b<n;b++){} } } }";
        assertThat(bigO(codigo)).isEqualTo("O(n^2)");
    }

    @Test
    void acumuladorMultiplicativoNoCorpoNaoViraLogaritmico() {
        String codigo = "class A { long m(int n) { long fat = 1; for (int i=1;i<=n;i++){ fat *= i; } return fat; } }";
        assertThat(bigO(codigo)).isEqualTo("O(n)");
    }

    @Test
    void halvingComAtribuicaoSimplesEhLogaritmico() {
        assertThat(bigO("class A { void m(int n) { for (int i = n; i > 0; i = i >> 1) {} } }"))
                .isEqualTo("O(log n)");
    }
}
