package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

/** A leitura do gabarito e do manifesto, sobre JSON sintetico escrito a mao (nada do corpus real). */
class GabaritoExternoTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String GABARITO = """
            {
              "campoExtra": "ignorado",
              "casos": {
                "java": [
                  {"arquivo": "021-leetcode-3022.java", "plataforma": "leetcode", "problema": "3022",
                   "tempoGabarito": "O(n * B)", "espacoGabarito": "O(1)", "tipoTempo": "pior-caso",
                   "leitura1": {"tempo": "x"}, "decisao": "ignorada"},
                  {"arquivo": "009-leetcode-2097.java", "plataforma": "leetcode", "problema": "2097",
                   "tempoGabarito": "O(n)", "espacoGabarito": null, "tipoTempo": "esperado"}
                ],
                "c": [
                  {"arquivo": "315-leetcode-2225.c", "plataforma": "leetcode", "problema": "2225",
                   "tempoGabarito": "O(m log m)", "espacoGabarito": "O(m)", "tipoTempo": "pior-caso"}
                ]
              }
            }
            """;

    private static final String MANIFESTO = """
            {
              "java": {"percorridos": 10, "aceitos": 2,
                       "exclusoesPorMotivo": {"sem-fonte": 5, "divergencia": 3},
                       "casos": [
                         {"solucao": {"arquivoLocal": "021-leetcode-3022.java", "sha256": "aa",
                                      "anterior": {"sha256": "descartado"}}},
                         {"solucao": {"arquivoLocal": "009-leetcode-2097.java", "sha256": "bb"}}
                       ]},
              "c": {"percorridos": 4, "aceitos": 1, "exclusoesPorMotivo": {"sem-solucao": 3},
                    "casos": [{"solucao": {"arquivoLocal": "315-leetcode-2225.c", "sha256": "cc"}}]}
            }
            """;

    @Test
    void leOsCasosPorLinguagemIgnorandoCamposExtras() throws IOException {
        GabaritoExterno gabarito = GabaritoExterno.de(JSON.readTree(GABARITO));

        assertThat(gabarito.casos(LinguagemProgramacao.JAVA)).hasSize(2);
        assertThat(gabarito.casos(LinguagemProgramacao.C)).hasSize(1);
        assertThat(gabarito.arquivos(LinguagemProgramacao.JAVA))
                .containsExactlyInAnyOrder("021-leetcode-3022.java", "009-leetcode-2097.java");
    }

    @Test
    void aplicaAReducaoNaNotaSemMexerNoGabarito() throws IOException {
        CasoExterno caso = GabaritoExterno.de(JSON.readTree(GABARITO)).casos(LinguagemProgramacao.C).get(0);

        assertThat(caso.tempoGabarito()).isEqualTo("O(m log m)");
        assertThat(caso.tempoNota()).isEqualTo("O(n log n)");
        assertThat(caso.espacoGabarito()).isEqualTo("O(m)");
        assertThat(caso.espacoNota()).isEqualTo("O(n)");
        assertThat(caso.tempoReduzido()).isTrue();
        assertThat(caso.espacoReduzido()).isTrue();
        assertThat(caso.tempoNaEscala()).isTrue();
        assertThat(caso.espacoNaEscala()).isTrue();
    }

    @Test
    void derivaIdentificacaoEMarcaTempoEsperado() throws IOException {
        CasoExterno esperado = GabaritoExterno.de(JSON.readTree(GABARITO)).casos(LinguagemProgramacao.JAVA).get(1);

        assertThat(esperado.id()).isEqualTo("009-2097");
        assertThat(esperado.tempoEsperado()).isTrue();
        assertThat(esperado.tempoReduzido()).isFalse();
    }

    @Test
    void espacoSemGabaritoFicaNuloEForaDaEscala() throws IOException {
        CasoExterno caso = GabaritoExterno.de(JSON.readTree(GABARITO)).casos(LinguagemProgramacao.JAVA).get(1);

        assertThat(caso.espacoGabarito()).isNull();
        assertThat(caso.espacoNota()).isNull();
        assertThat(caso.espacoNaEscala()).isFalse();
        assertThat(caso.espacoReduzido()).isFalse();
    }

    @Test
    void gabaritoForaDaTabelaDeReducaoFalhaAlto() throws IOException {
        JsonNode adulterado = JSON.readTree(GABARITO.replace("\"O(n * B)\"", "\"O(n)\""));

        assertThatThrownBy(() -> GabaritoExterno.de(adulterado))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("021-leetcode-3022.java");
    }

    @Test
    void campoObrigatorioAusenteFalhaAlto() throws IOException {
        JsonNode semProblema = JSON.readTree(GABARITO.replace("\"problema\": \"2225\",", ""));

        assertThatThrownBy(() -> GabaritoExterno.de(semProblema))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("problema");
    }

    @Test
    void manifestoExtraiSoOHashDeSolucao() throws IOException {
        ManifestoExterno manifesto = ManifestoExterno.de(JSON.readTree(MANIFESTO));

        assertThat(manifesto.sha256PorArquivo(LinguagemProgramacao.JAVA))
                .containsOnly(
                        entry("021-leetcode-3022.java", "aa"),
                        entry("009-leetcode-2097.java", "bb"));
        assertThat(manifesto.sha256PorArquivo(LinguagemProgramacao.C)).containsEntry("315-leetcode-2225.c", "cc");
    }

    @Test
    void manifestoLeAsContagensDoFunil() throws IOException {
        ManifestoExterno.Contagens java = ManifestoExterno.de(JSON.readTree(MANIFESTO)).contagens(LinguagemProgramacao.JAVA);

        assertThat(java.percorridos()).isEqualTo(10);
        assertThat(java.aceitos()).isEqualTo(2);
        assertThat(java.exclusoesPorMotivo()).containsEntry("sem-fonte", 5).containsEntry("divergencia", 3);
    }

    @Test
    void manifestoComArquivoRepetidoFalhaAlto() throws IOException {
        JsonNode repetido = JSON.readTree(MANIFESTO.replace("009-leetcode-2097.java", "021-leetcode-3022.java"));

        assertThatThrownBy(() -> ManifestoExterno.de(repetido))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("021-leetcode-3022.java");
    }
}
