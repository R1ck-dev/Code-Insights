package com.projeto.codeinsights.infrastructure.metrica.c;

import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.infrastructure.metrica.MetricaCalculada;

/**
 * SPI de uma metrica calculada sobre um fonte C, espelhando o {@code AnalisadorMetricaJava}: cada
 * metrica e um bean isolado, e acrescentar uma metrica e acrescentar uma implementacao.
 * <p>
 * Existir como interface propria — em vez de reaproveitar a de Java — e o que permite ao
 * {@link AnalisadorDeC} derivar {@code metricasSuportadas} dos beans registrados. Uma lista escrita
 * a mao envelheceria calada: quem acrescentasse a metrica e esquecesse a lista teria o motor
 * calculando um numero que a plataforma jura nao existir.
 */
public interface AnalisadorMetricaC {

    TipoMetrica tipo();

    MetricaCalculada analisar(CodigoDeC codigo);
}
