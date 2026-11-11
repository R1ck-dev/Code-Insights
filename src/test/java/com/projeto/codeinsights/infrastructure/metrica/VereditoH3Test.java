package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** As quatro posicoes do Adendo 6, item 7, com os limiares de significancia do Adendo 7, item 5. */
class VereditoH3Test {

    @Test
    void menorComPAbaixoDeAlfaSustenta() {
        assertThat(VereditoH3.de(54, 55, 12, 15)).isEqualTo(VereditoH3.SUSTENTADA);
        assertThat(VereditoH3.de(37, 40, 12, 18)).isEqualTo(VereditoH3.SUSTENTADA);
    }

    @Test
    void menorComPAcimaDeAlfaEInconclusivo() {
        assertThat(VereditoH3.de(54, 55, 13, 15)).isEqualTo(VereditoH3.INCONCLUSIVO);
        assertThat(VereditoH3.de(37, 40, 13, 18)).isEqualTo(VereditoH3.INCONCLUSIVO);
    }

    @Test
    void limiarDeJavaNoDenominadorDoCorpusExterno() {
        assertThat(VereditoH3.de(54, 55, 13, 16)).isEqualTo(VereditoH3.SUSTENTADA);
        assertThat(VereditoH3.de(54, 55, 14, 16)).isEqualTo(VereditoH3.INCONCLUSIVO);
    }

    @Test
    void taxaIgualNaoSustenta() {
        assertThat(VereditoH3.de(54, 55, 54, 55)).isEqualTo(VereditoH3.NAO_SUSTENTADA);
        assertThat(VereditoH3.de(54, 55, 108, 110)).isEqualTo(VereditoH3.NAO_SUSTENTADA);
    }

    @Test
    void taxaMaiorNaoSustenta() {
        assertThat(VereditoH3.de(37, 40, 18, 18)).isEqualTo(VereditoH3.NAO_SUSTENTADA);
    }

    @Test
    void denominadorVazioFalhaAlto() {
        assertThatThrownBy(() -> VereditoH3.de(54, 55, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
