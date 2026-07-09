package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.javaparser.ast.CompilationUnit;
import com.projeto.codeinsights.infrastructure.metrica.CorpusDeAlgoritmos.Caso;

/**
 * Valida o motor contra o gabarito de {@link CorpusDeAlgoritmos}. Cada caso e um
 * algoritmo canonico cuja complexidade a literatura ja fixou; o motor precisa chegar
 * na mesma classe. E a medida objetiva de "quao assertivo" o motor e - e o que impede
 * que um ajuste numa heuristica quebre silenciosamente outra.
 */
class MotorDeMetricasCorpusTest {

    private final BigOTempoAnalisador tempo = new BigOTempoAnalisador();
    private final EspacoAnalisador espaco = new EspacoAnalisador();

    static List<Caso> casos() {
        return CorpusDeAlgoritmos.casos();
    }

    @ParameterizedTest(name = "tempo: {0}")
    @MethodSource("casos")
    void complexidadeDeTempoBateComOGabarito(Caso caso) {
        CompilationUnit unidade = AnalisadorTestSupport.parse(caso.codigo());
        MetricaCalculada calculada = tempo.analisar(unidade);

        assertThat(calculada.rotulo())
                .as("%s -> %s", caso.nome(), calculada.detalhe())
                .isEqualTo(caso.tempoEsperado());
    }

    @ParameterizedTest(name = "espaco: {0}")
    @MethodSource("casos")
    void complexidadeDeEspacoBateComOGabarito(Caso caso) {
        if (caso.espacoEsperado() == null) {
            return;
        }
        CompilationUnit unidade = AnalisadorTestSupport.parse(caso.codigo());
        MetricaCalculada calculada = espaco.analisar(unidade);

        assertThat(calculada.rotulo())
                .as("%s -> %s", caso.nome(), calculada.detalhe())
                .isEqualTo(caso.espacoEsperado());
    }

    @Test
    void todoCasoDoCorpusParseia() {
        assertThatCode(() -> CorpusDeAlgoritmos.casos()
                .forEach(caso -> AnalisadorTestSupport.parse(caso.codigo())))
                .doesNotThrowAnyException();
    }
}
