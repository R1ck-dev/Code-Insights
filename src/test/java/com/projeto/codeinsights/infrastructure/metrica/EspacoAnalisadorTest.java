package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EspacoAnalisadorTest {

    private final EspacoAnalisador analisador = new EspacoAnalisador();

    private String espaco(String codigo) {
        return analisador.analisar(AnalisadorTestSupport.parse(codigo)).rotulo();
    }

    @Test
    void semAlocacaoEhConstante() {
        assertThat(espaco("class A { void m() { int x = 1; } }")).isEqualTo("O(1)");
    }

    @Test
    void arrayDeTamanhoConstanteEhConstante() {
        assertThat(espaco("class A { void m() { int[] a = new int[100]; } }")).isEqualTo("O(1)");
    }

    @Test
    void arrayDimensionadoPelaEntradaEhLinear() {
        assertThat(espaco("class A { void m(int n) { int[] a = new int[n]; } }")).isEqualTo("O(n)");
    }

    @Test
    void matrizDimensionadaPelaEntradaEhQuadratica() {
        assertThat(espaco("class A { void m(int n) { int[][] a = new int[n][n]; } }")).isEqualTo("O(n^2)");
    }

    @Test
    void colecaoDinamicaEhLinear() {
        assertThat(espaco("class A { void m() { java.util.List<Integer> l = new java.util.ArrayList<>(); } }"))
                .isEqualTo("O(n)");
    }

    @Test
    void recursaoContaComoPilhaLinear() {
        assertThat(espaco("class A { int f(int n) { if (n == 0) return 0; return f(n - 1); } }"))
                .isEqualTo("O(n)");
    }
}
