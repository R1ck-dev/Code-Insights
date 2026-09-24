package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;

/**
 * Defeitos que a <b>segunda</b> revisao adversarial do motor de Java (F1.2, 24/09/2026) encontrou,
 * cada um confirmado por execucao antes de entrar aqui.
 * <p>
 * Os da primeira rodada estao em {@link DefeitosDoMotorDeJavaTest}; as rodadas ficam separadas
 * porque sao relatadas com numeros proprios. Aqui os casos vivem em arquivo, como no corpus
 * ({@code src/test/resources/defeitos/java-rodada-2/}), porque sao dezenas de programas completos:
 * o manifesto guarda a metrica, o gabarito e a justificativa de cada um.
 * <p>
 * Todo caso afirma o <b>gabarito</b>. Os abertos levam {@code @Tag("defeito-aberto")} e ficam fora
 * do build normal; os controles — escritas do mesmo algoritmo que o motor ja acerta — rodam sempre,
 * para que a correcao de um defeito nao quebre a escrita que ja estava certa. Corrigir um defeito e
 * trocar os seus {@code "aberto": true} para {@code false} no manifesto.
 */
class DefeitosDoMotorDeJavaRodada2Test {

    private static final String PASTA = "/defeitos/java-rodada-2/";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JavaParserAnalisadorMetricas analisador = new JavaParserAnalisadorMetricas(List.of(
            new BigOTempoAnalisador(), new EspacoAnalisador(), new CiclomaticaAnalisador()));

    record Caso(String defeito, String arquivo, String metrica, String gabarito, boolean aberto, String nota) {

        @Override
        public String toString() {
            return defeito + " " + arquivo + " [" + metrica + " = " + gabarito + "]";
        }
    }

    private record Manifesto(String descricao, List<Caso> casos) {
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("abertos")
    @Tag("defeito-aberto")
    void defeitoAberto(Caso caso) {
        assertThat(medir(caso)).as(caso.nota()).isEqualTo(caso.gabarito());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fechados")
    void controleOuCorrigido(Caso caso) {
        assertThat(medir(caso)).as(caso.nota()).isEqualTo(caso.gabarito());
    }

    static Stream<Caso> abertos() {
        return casos().filter(Caso::aberto);
    }

    static Stream<Caso> fechados() {
        return casos().filter(caso -> !caso.aberto());
    }

    private String medir(Caso caso) {
        List<ResultadoMetrica> resultados = analisador.analisar(new Resolucao(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), ler(caso.arquivo()), LinguagemProgramacao.JAVA, 1, null));
        return switch (caso.metrica()) {
            case "tempo" -> metrica(resultados, TipoMetrica.BIG_O_TEMPO).getRotulo();
            case "espaco" -> metrica(resultados, TipoMetrica.COMPLEXIDADE_ESPACO).getRotulo();
            case "ciclomatica" -> String.valueOf(metrica(resultados, TipoMetrica.COMPLEXIDADE_CICLOMATICA).getValor());
            default -> throw new IllegalArgumentException("metrica desconhecida no manifesto: " + caso.metrica());
        };
    }

    private static ResultadoMetrica metrica(List<ResultadoMetrica> resultados, TipoMetrica tipo) {
        return resultados.stream()
                .filter(resultado -> resultado.getTipo() == tipo)
                .findFirst()
                .orElseThrow(() -> new AssertionError("o motor nao produziu " + tipo));
    }

    private static Stream<Caso> casos() {
        try (InputStream entrada = abrir(PASTA + "manifesto.json")) {
            return JSON.readValue(entrada, Manifesto.class).casos().stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String ler(String arquivo) {
        try (InputStream entrada = abrir(PASTA + arquivo)) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream abrir(String caminho) {
        InputStream entrada = DefeitosDoMotorDeJavaRodada2Test.class.getResourceAsStream(caminho);
        if (entrada == null) {
            throw new IllegalStateException("recurso ausente: " + caminho);
        }
        return entrada;
    }
}
