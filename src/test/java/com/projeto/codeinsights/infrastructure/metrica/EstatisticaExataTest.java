package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.projeto.codeinsights.infrastructure.metrica.EstatisticaExata.Intervalo;

/**
 * Valores de referencia do Adendo 6: as taxas de base do corpus proprio (54/55 e 37/40) contra os
 * acertos externos que decidem o veredito de H3. Os p vem da tabela do plano, arredondados a 4 casas.
 */
class EstatisticaExataTest {

    private static final double TOLERANCIA = 1e-4;

    @ParameterizedTest(name = "Fisher {0}/{1} x {2}/{3} = {4}")
    @CsvSource({
            "54, 55, 12, 15, 0.0288",
            "54, 55, 13, 15, 0.1138",
            "37, 40, 10, 15, 0.0278",
            "37, 40, 11, 15, 0.0790",
            "54, 55, 13, 16, 0.0336",
            "54, 55, 14, 16, 0.1253",
            "37, 40, 12, 18, 0.0197",
            "37, 40, 13, 18, 0.0925"
    })
    void fisherBateComOsValoresDoPlano(long a, long n1, long b, long n2, double esperado) {
        assertThat(EstatisticaExata.fisherBilateral(a, n1, b, n2)).isCloseTo(esperado, within(TOLERANCIA));
    }

    @Test
    void fisherNaoDependeDaOrdemDosGrupos() {
        assertThat(EstatisticaExata.fisherBilateral(12, 15, 54, 55))
                .isEqualTo(EstatisticaExata.fisherBilateral(54, 55, 12, 15));
    }

    @Test
    void tabelaDegeneradaTemPUm() {
        assertThat(EstatisticaExata.fisherBilateral(0, 10, 0, 12)).isEqualTo(1.0);
        assertThat(EstatisticaExata.fisherBilateral(10, 10, 12, 12)).isEqualTo(1.0);
    }

    @Test
    void fisherRejeitaTabelaImpossivel() {
        assertThatThrownBy(() -> EstatisticaExata.fisherBilateral(6, 5, 1, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "Wilson {0}/{1} = [{2}; {3}]")
    @CsvSource({
            "54, 55, 0.9039, 0.9968",
            "37, 40, 0.8014, 0.9742",
            "0, 10, 0.0, 0.2775",
            "1, 2, 0.0945, 0.9055"
    })
    void wilsonBateComOsValoresDoPlano(long k, long n, double inferior, double superior) {
        Intervalo intervalo = EstatisticaExata.wilson(k, n);

        assertThat(intervalo.inferior()).isCloseTo(inferior, within(TOLERANCIA));
        assertThat(intervalo.superior()).isCloseTo(superior, within(TOLERANCIA));
    }

    @Test
    void wilsonComTodosAcertosTerminaEmUmSemPassar() {
        Intervalo intervalo = EstatisticaExata.wilson(10, 10);

        assertThat(intervalo.superior()).isCloseTo(1.0, within(1e-12)).isLessThanOrEqualTo(1.0);
        assertThat(intervalo.inferior()).isBetween(0.0, 1.0);
    }

    @Test
    void wilsonRejeitaEntradaInvalida() {
        assertThatThrownBy(() -> EstatisticaExata.wilson(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EstatisticaExata.wilson(3, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EstatisticaExata.wilson(-1, 2)).isInstanceOf(IllegalArgumentException.class);
    }
}
