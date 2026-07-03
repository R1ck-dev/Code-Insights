package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CiclomaticaAnalisadorTest {

    private final CiclomaticaAnalisador analisador = new CiclomaticaAnalisador();

    private int complexidade(String codigo) {
        return analisador.analisar(AnalisadorTestSupport.parse(codigo)).valor();
    }

    @Test
    void metodoLinearSemDecisoesTemComplexidadeUm() {
        assertThat(complexidade("class A { void m() { int x = 1; } }")).isEqualTo(1);
    }

    @Test
    void umIfSomaUmaDecisao() {
        assertThat(complexidade("class A { void m(int x) { if (x > 0) {} } }")).isEqualTo(2);
    }

    @Test
    void operadorLogicoDeCurtoCircuitoContaComoDecisao() {
        assertThat(complexidade("class A { void m(int a, int b) { if (a > 0 && b > 0) {} } }")).isEqualTo(3);
    }

    @Test
    void cadaRotuloDeCaseContaMasDefaultNao() {
        String codigo = "class A { void m(int x) { switch (x) { case 1: break; case 2: break; default: break; } } }";
        assertThat(complexidade(codigo)).isEqualTo(3);
    }

    @Test
    void somaSobreTodosOsMetodos() {
        String codigo = "class A { void m(boolean a) { if (a) {} } void n(boolean b) { if (b) {} } }";
        assertThat(complexidade(codigo)).isEqualTo(4);
    }

    @Test
    void caseComRotulosAgrupadosContaCadaRotulo() {
        String codigo = "class A { void m(int x) { switch (x) { case 1, 2, 3 -> {} default -> {} } } }";
        assertThat(complexidade(codigo)).isEqualTo(4);
    }
}
