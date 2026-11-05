package com.projeto.codeinsights.infrastructure.metrica;

import static com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.PADRAO;
import static com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.PRIMEIRO_JAVA;
import static com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.SEGUNDO_JAVA;
import static com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.TOKEN;
import static com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.UNICO_C;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.Arquivo;

/**
 * A carga do corpus externo, sobre arquivos sinteticos em {@code @TempDir}: hash conferido antes de
 * devolver qualquer codigo, conjunto igual ao do gabarito, e nenhuma mensagem com o conteudo.
 */
class CorpusExternoLocalTest {

    @TempDir
    Path pasta;

    private List<FonteVerificada> carregar(List<Arquivo> gabarito, List<Arquivo> naPasta, JsonNode manifesto) {
        CorpusExternoSintetico.gravar(pasta, naPasta);
        return CorpusExternoLocal.carregar(pasta, GabaritoExterno.de(CorpusExternoSintetico.gabarito(gabarito)),
                ManifestoExterno.de(manifesto));
    }

    private List<FonteVerificada> carregarPadrao() {
        return carregar(PADRAO, PADRAO, CorpusExternoSintetico.manifesto(PADRAO));
    }

    @Test
    void devolveOsTresFontesNaOrdemDoGabaritoComOCodigoDecodificado() {
        List<FonteVerificada> fontes = carregarPadrao();

        assertThat(fontes).extracting(f -> f.caso().arquivo())
                .containsExactly(PRIMEIRO_JAVA.nome(), SEGUNDO_JAVA.nome(), UNICO_C.nome());
        assertThat(fontes.get(0).codigo()).isEqualTo(PRIMEIRO_JAVA.conteudo()).contains("ação");
        assertThat(fontes.get(2).codigo()).isEqualTo(UNICO_C.conteudo());
    }

    @Test
    void hashErradoFalhaSemDevolverCodigoECitaSoArquivoEHashes() {
        JsonNode manifesto = CorpusExternoSintetico.manifesto(PADRAO, Map.of(SEGUNDO_JAVA.nome(), "0".repeat(64)));

        assertThatThrownBy(() -> carregar(PADRAO, PADRAO, manifesto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(SEGUNDO_JAVA.nome())
                .hasMessageContaining("0".repeat(64))
                .hasMessageContaining(CorpusExternoSintetico.sha256(SEGUNDO_JAVA.conteudo()))
                .hasMessageNotContaining(TOKEN)
                .hasMessageNotContaining("class Soma");
    }

    @Test
    void hashErradoNaLinguagemSeguinteTambemImpedeDevolverOsDaPrimeira() {
        JsonNode manifesto = CorpusExternoSintetico.manifesto(PADRAO, Map.of(UNICO_C.nome(), "f".repeat(64)));

        assertThatThrownBy(() -> carregar(PADRAO, PADRAO, manifesto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(UNICO_C.nome())
                .hasMessageNotContaining(TOKEN);
    }

    @Test
    void arquivoFaltandoNaPastaFalhaCitandoONome() {
        assertThatThrownBy(() -> carregar(PADRAO, List.of(PRIMEIRO_JAVA, UNICO_C), CorpusExternoSintetico.manifesto(PADRAO)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("faltando")
                .hasMessageContaining(SEGUNDO_JAVA.nome())
                .hasMessageNotContaining(TOKEN);
    }

    @Test
    void arquivoExtraNaPastaFalhaCitandoONome() {
        Arquivo extra = new Arquivo(UNICO_C.linguagem(), "004-leetcode-44.c", CorpusExternoSintetico.C_VALIDO);
        List<Arquivo> naPasta = List.of(PRIMEIRO_JAVA, SEGUNDO_JAVA, UNICO_C, extra);

        assertThatThrownBy(() -> carregar(PADRAO, naPasta, CorpusExternoSintetico.manifesto(PADRAO)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("a mais")
                .hasMessageContaining(extra.nome())
                .hasMessageNotContaining(TOKEN);
    }

    @Test
    void subpastaDentroDaPastaDaLinguagemContaComoItemAMais() throws IOException {
        CorpusExternoSintetico.gravar(pasta, PADRAO);
        Files.createDirectory(pasta.resolve("java").resolve("descartados"));

        assertThatThrownBy(() -> CorpusExternoLocal.carregar(pasta,
                GabaritoExterno.de(CorpusExternoSintetico.gabarito(PADRAO)),
                ManifestoExterno.de(CorpusExternoSintetico.manifesto(PADRAO))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("descartados");
    }

    @Test
    void pastaDaLinguagemAusenteFalha() {
        assertThatThrownBy(() -> CorpusExternoLocal.carregar(pasta,
                GabaritoExterno.de(CorpusExternoSintetico.gabarito(PADRAO)),
                ManifestoExterno.de(CorpusExternoSintetico.manifesto(PADRAO))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ausente");
    }

    @Test
    void manifestoSemHashDeUmCasoDoGabaritoFalha() {
        List<Arquivo> semUm = List.of(PRIMEIRO_JAVA, UNICO_C);

        assertThatThrownBy(() -> carregar(PADRAO, PADRAO, CorpusExternoSintetico.manifesto(semUm)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(SEGUNDO_JAVA.nome())
                .hasMessageNotContaining(TOKEN);
    }

    @Test
    void toStringDaFonteEDoCasoNaoTrazemOCodigo() {
        FonteVerificada fonte = carregarPadrao().get(0);

        assertThat(fonte.toString()).contains(PRIMEIRO_JAVA.nome()).doesNotContain(TOKEN).doesNotContain("class Soma");
        assertThat(fonte.caso().toString()).doesNotContain(TOKEN);
    }
}
