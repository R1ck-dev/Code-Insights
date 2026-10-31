package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;

/**
 * Caracterizacao (IC-11b): o que o motor de Java responde HOJE para o D6, e que a resolucao do
 * ciclo no tempo nao pode mexer.
 * <p>
 * Diferente do manifesto, aqui o teste afirma o que o motor <b>faz</b>, nao o gabarito. O D6
 * ({@code outro.ehImpar()} com dois campos {@code outro} de tipos diferentes) segue defeito aberto
 * no manifesto, porque a tabela de tipos e plana e a chamada nao resolve: a recursao mutua some e o
 * motor responde O(1) (o real e O(n)). A 11b monta o grafo de chamadas pela mesma resolucao; se um
 * dia o D6 for corrigido, estes numeros mudam de proposito e o teste e atualizado junto com o
 * manifesto. Ate la, ele impede que a 11b o altere por acidente.
 */
class RecursaoMutuaBaselineTest {

    private static final String D6 = "/defeitos/java-rodada-2/d6-recursao-mutua-campo-homonimo.java";

    private final JavaParserAnalisadorMetricas analisador = new JavaParserAnalisadorMetricas(List.of(
            new BigOTempoAnalisador(), new EspacoAnalisador(), new CiclomaticaAnalisador()));

    @Test
    @DisplayName("D6: sem resolver o receptor, o tempo segue O(1) e nao vira ciclo")
    void d6TempoNaoMuda() {
        assertThat(rotulo(TipoMetrica.BIG_O_TEMPO)).isEqualTo("O(1)");
    }

    @Test
    @DisplayName("D6: o espaco segue O(1)")
    void d6EspacoNaoMuda() {
        assertThat(rotulo(TipoMetrica.COMPLEXIDADE_ESPACO)).isEqualTo("O(1)");
    }

    private String rotulo(TipoMetrica tipo) {
        List<ResultadoMetrica> resultados = analisador.analisar(new Resolucao(UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), ler(D6), LinguagemProgramacao.JAVA, 1, null));
        return resultados.stream()
                .filter(resultado -> resultado.getTipo() == tipo)
                .findFirst()
                .orElseThrow(() -> new AssertionError("o motor nao produziu " + tipo))
                .getRotulo();
    }

    private static String ler(String caminho) {
        try (InputStream entrada = RecursaoMutuaBaselineTest.class.getResourceAsStream(caminho)) {
            if (entrada == null) {
                throw new IllegalStateException("recurso ausente: " + caminho);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
