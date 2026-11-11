package com.projeto.codeinsights.infrastructure.metrica;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

/**
 * Leitura das anotacoes do corpus externo commitadas em {@code src/test/resources/externo/}: o
 * gabarito e o manifesto de proveniencia. Sao anotacao sobre os arquivos, nao codigo de terceiros, e
 * tem campos extras que o modelo ignora, por isso a leitura e por {@link JsonNode}.
 */
final class AnotacaoJson {

    private static final ObjectMapper JSON = new ObjectMapper();

    private AnotacaoJson() {
    }

    static JsonNode ler(String recurso) {
        try (InputStream entrada = AnotacaoJson.class.getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new IllegalStateException("Recurso ausente do classpath: " + recurso);
            }
            return JSON.readTree(entrada);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler " + recurso + ".", e);
        }
    }

    /** Chave da linguagem nos dois JSON: {@code java} ou {@code c}. */
    static String chave(LinguagemProgramacao linguagem) {
        return linguagem.name().toLowerCase(Locale.ROOT);
    }

    /** Texto do campo, ou {@code null} quando ele falta ou e {@code null} no JSON. */
    static String textoOuNulo(JsonNode no, String campo) {
        JsonNode valor = no.path(campo);
        return valor.isMissingNode() || valor.isNull() ? null : valor.asText();
    }

    /** Texto do campo; falha com o nome do campo quando ele falta. */
    static String texto(JsonNode no, String campo) {
        String valor = textoOuNulo(no, campo);
        if (valor == null) {
            throw new IllegalStateException("Campo obrigatorio ausente na anotacao: " + campo);
        }
        return valor;
    }
}
