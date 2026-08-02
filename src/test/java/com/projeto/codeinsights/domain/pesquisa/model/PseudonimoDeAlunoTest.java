package com.projeto.codeinsights.domain.pesquisa.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class PseudonimoDeAlunoTest {

    @Test
    void oMesmoAutorRecebeSempreOMesmoCodigo() {
        UUID autor = UUID.fromString("11111111-2222-3333-4444-555555555555");

        assertThat(PseudonimoDeAluno.de(autor)).isEqualTo(PseudonimoDeAluno.de(autor));
    }

    /**
     * A estabilidade nao pode depender da instancia da JVM nem da ordem de leitura: o codigo
     * gravado num export de marco tem que continuar valendo em outubro. Este teste prende o valor
     * concreto — se a derivacao mudar, todo export anterior deixa de casar, e isso precisa ser uma
     * decisao e nao um efeito colateral.
     */
    @Test
    void aDerivacaoEhFixaEntreExecucoes() {
        UUID autor = UUID.fromString("11111111-2222-3333-4444-555555555555");

        assertThat(PseudonimoDeAluno.de(autor)).isEqualTo("A-666FF6");
    }

    @Test
    void autoresDistintosRecebemCodigosDistintos() {
        Set<String> codigos = new HashSet<>();
        IntStream.range(0, 500).forEach(i -> codigos.add(PseudonimoDeAluno.de(UUID.randomUUID())));

        assertThat(codigos).hasSize(500);
    }

    @Test
    void oCodigoTemPrefixoEComprimentoEstaveis() {
        assertThat(PseudonimoDeAluno.de(UUID.randomUUID()))
                .startsWith("A-")
                .hasSize(8)
                .matches("A-[0-9A-F]{6}");
    }

    @Test
    void naoHaPseudonimoParaAutorNulo() {
        assertThatThrownBy(() -> PseudonimoDeAluno.de(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
