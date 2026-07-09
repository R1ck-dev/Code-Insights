package com.projeto.codeinsights.domain.knowledge.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

class ResultadoMetricaTest {

    @Test
    void criacaoGeraIdECarimbaInstante() {
        ResultadoMetrica resultado = new ResultadoMetrica(
                null, UUID.randomUUID(), TipoMetrica.BIG_O_TEMPO, 4, "O(n^2)", "detalhe", NivelConfianca.ALTA);

        assertThat(resultado.getId()).isNotNull();
        assertThat(resultado.getAnalisadoEm()).isNotNull();
        assertThat(resultado.getValor()).isEqualTo(4);
        assertThat(resultado.getRotulo()).isEqualTo("O(n^2)");
        assertThat(resultado.getConfianca()).isEqualTo(NivelConfianca.ALTA);
    }

    @Test
    void resolucaoNulaEhInvalida() {
        assertThatThrownBy(() -> new ResultadoMetrica(
                null, null, TipoMetrica.BIG_O_TEMPO, 1, "O(1)", null, NivelConfianca.ALTA))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void tipoNuloEhInvalido() {
        assertThatThrownBy(() -> new ResultadoMetrica(
                null, UUID.randomUUID(), null, 1, "O(1)", null, NivelConfianca.ALTA))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void rotuloEmBrancoEhInvalido() {
        assertThatThrownBy(() -> new ResultadoMetrica(
                null, UUID.randomUUID(), TipoMetrica.COMPLEXIDADE_CICLOMATICA, 1, "  ", null, NivelConfianca.ALTA))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void confiancaNulaEhInvalida() {
        assertThatThrownBy(() -> new ResultadoMetrica(
                null, UUID.randomUUID(), TipoMetrica.BIG_O_TEMPO, 2, "O(n)", null, null))
                .isInstanceOf(NegocioException.class);
    }
}
