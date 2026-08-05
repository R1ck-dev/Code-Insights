package com.projeto.codeinsights.infrastructure.persistence.pesquisa.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Congela o TCLE em vigor: a <b>versao</b> e o <b>texto</b>, os dois presos por este teste.
 * <p>
 * Consentimento e dado a uma VERSAO. Publicar uma versao nova invalida todos os aceites anteriores
 * e a coorte volta a zero ate cada participante responder de novo — ja aconteceu uma vez no banco
 * local, ao promover {@code v0-rascunho} para {@code v1}. Num piloto com recrutamento presencial,
 * perder a coorte no meio significa reabordar cada pessoa.
 * <p>
 * O mecanismo de versionamento <b>continua existindo</b>, e de proposito: quando o termo mudar de
 * verdade — outro uso dos dados, outro risco, outra duracao — pedir consentimento de novo e
 * exigencia etica, nao inconveniencia. O que este teste elimina e a mudanca por <b>acidente</b>:
 * corrigir uma virgula no texto tem hoje o mesmo efeito de reescrever o termo inteiro, e nada
 * avisava. Agora avisa, e trocar exige um gesto deliberado de quem sabe o preco.
 * <p>
 * <b>Ao publicar uma versao nova de verdade:</b> atualize {@link #VERSAO_CONGELADA} e
 * {@link #SHA256_DO_TEXTO}, e trate a perda da coorte como parte do plano — nao como surpresa.
 */
class TermoDeConsentimentoCongeladoTest {

    /** Versao vigente. Precisa bater com o default de {@code app.pesquisa.consentimento.versao}. */
    private static final String VERSAO_CONGELADA = "v1";

    /** SHA-256 de {@code consentimento/termo-v1.md}, congelado em 2026-08-05. */
    private static final String SHA256_DO_TEXTO =
            "be54d7c10e8580be9932972a7e6a7cf25fd020b787eb3e598297eb04d4230af2";

    private static final Path APPLICATION_YML = Path.of("src", "main", "resources", "application.yml");

    private static final Pattern VERSAO_NO_YML = Pattern.compile(
            "versao:\\s*\\$\\{TERMO_CONSENTIMENTO_VERSAO:([^}]+)\\}");

    @Test
    @DisplayName("o texto do termo em vigor nao mudou")
    void oTextoDoTermoNaoMudou() throws Exception {
        String caminho = "/consentimento/termo-%s.md".formatted(VERSAO_CONGELADA);

        assertThat(sha256De(caminho))
                .as("""
                        O texto de %s mudou.

                        Consentimento e dado A UMA VERSAO: se este texto foi alterado, todo aceite ja \
                        registrado deixou de valer para o texto novo, e a coorte esvazia ate cada \
                        participante responder outra vez.

                        Se a mudanca for INTENCIONAL, publique um arquivo termo-<nova versao>.md, mova \
                        app.pesquisa.consentimento.versao e atualize VERSAO_CONGELADA e SHA256_DO_TEXTO \
                        aqui. Se for um ajuste de redacao que voce nao quer cobrar de ninguem, desfaca: \
                        nao ha como alterar o texto sem invalidar os aceites.""", caminho)
                .isEqualTo(SHA256_DO_TEXTO);
    }

    @Test
    @DisplayName("a versao configurada e a versao congelada")
    void aVersaoConfiguradaEhACongelada() throws IOException {
        String yml = Files.readString(APPLICATION_YML, StandardCharsets.UTF_8);
        Matcher achado = VERSAO_NO_YML.matcher(yml);

        assertThat(achado.find())
                .as("nao achei o default de app.pesquisa.consentimento.versao em %s", APPLICATION_YML)
                .isTrue();

        assertThat(achado.group(1).trim())
                .as("""
                        O default de app.pesquisa.consentimento.versao saiu de %s.

                        Trocar a versao invalida os aceites existentes e zera a coorte. Se for \
                        deliberado, atualize VERSAO_CONGELADA e SHA256_DO_TEXTO neste teste.""",
                        VERSAO_CONGELADA)
                .isEqualTo(VERSAO_CONGELADA);
    }

    /**
     * O arquivo da versao congelada precisa existir no classpath — e o que
     * {@code TermoDeConsentimentoClasspathAdapter} carrega no boot, e sem ele a aplicacao nem sobe.
     */
    @Test
    @DisplayName("o arquivo da versao congelada existe e nao esta vazio")
    void oArquivoExiste() throws IOException {
        try (InputStream entrada = abrir("/consentimento/termo-%s.md".formatted(VERSAO_CONGELADA))) {
            assertThat(new String(entrada.readAllBytes(), StandardCharsets.UTF_8)).isNotBlank();
        }
    }

    private static String sha256De(String caminho) throws IOException, NoSuchAlgorithmException {
        try (InputStream entrada = abrir(caminho)) {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(entrada.readAllBytes());
            return HexFormat.of().formatHex(resumo);
        }
    }

    private static InputStream abrir(String caminho) {
        InputStream entrada = TermoDeConsentimentoCongeladoTest.class.getResourceAsStream(caminho);
        if (entrada == null) {
            throw new IllegalStateException("Termo de consentimento nao encontrado: " + caminho);
        }
        return entrada;
    }
}
