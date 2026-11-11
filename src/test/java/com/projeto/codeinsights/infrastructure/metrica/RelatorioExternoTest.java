package com.projeto.codeinsights.infrastructure.metrica;

import static com.projeto.codeinsights.infrastructure.metrica.MedidasSinteticas.FORA_DA_ESCALA;
import static com.projeto.codeinsights.infrastructure.metrica.MedidasSinteticas.caso;
import static com.projeto.codeinsights.infrastructure.metrica.MedidasSinteticas.recusada;
import static com.projeto.codeinsights.infrastructure.metrica.MedidasSinteticas.respondida;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.infrastructure.metrica.CasoDeCorpus.Categoria;
import com.projeto.codeinsights.infrastructure.metrica.MedidaExterna.MotivoRecusa;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Medida;

/**
 * O texto do bloco externo, gerado de medidas sinteticas: os numeros (taxa, Wilson, Fisher), o
 * veredito so como o {@link VereditoH3} o devolve, e os limites fixos que valem para qualquer
 * resultado.
 */
class RelatorioExternoTest {

    private static final LinguagemProgramacao JAVA = LinguagemProgramacao.JAVA;
    private static final LinguagemProgramacao C = LinguagemProgramacao.C;
    private static final LocalDate DATA = LocalDate.of(2026, 11, 6);
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Java com {@code acertosJava} de 16 mensuraveis e C com {@code acertosC} de 18, mais os fora da escala. */
    private static String bloco(int acertosJava, int acertosC) {
        List<MedidaExterna> externas = new ArrayList<>(MedidasSinteticas.lote(JAVA, acertosJava, 16, 4));
        externas.addAll(MedidasSinteticas.lote(C, acertosC, 18, 2));
        return RelatorioExterno.renderizar(dados(externas));
    }

    private static DadosDoBloco dados(List<MedidaExterna> externas) {
        return new DadosDoBloco(externas, MedidasSinteticas.proprio(54, 55, 37, 40), manifesto(), DATA);
    }

    private static ManifestoExterno manifesto() {
        ObjectNode raiz = JSON.createObjectNode();
        ObjectNode java = raiz.putObject("java");
        java.put("percorridos", 135).put("aceitos", 20);
        java.putObject("exclusoesPorMotivo").put("sem-solucao", 12).put("divergencia", 32);
        ObjectNode c = raiz.putObject("c");
        c.put("percorridos", 535).put("aceitos", 20);
        c.putObject("exclusoesPorMotivo").put("inelegivel", 42).put("divergencia", 80);
        return ManifestoExterno.de(raiz);
    }

    // ---------------------------------------------------------------- numeros e veredito

    @Test
    void taxasDeTempoMostramPrincipalWilsonSensibilidadeECobertura() {
        String bloco = bloco(13, 12);

        assertThat(bloco.lines().toList())
                .contains("| JAVA | proprio | 54/55 (98.2%) | [90.4%; 99.7%] | 54/55 (98.2%) | 55/55 (100.0%) |",
                        "| JAVA | EXTERNO | 13/16 (81.3%) | [57.0%; 93.4%] | 13/20 (65.0%) | 16/20 (80.0%) |",
                        "| C | proprio | 37/40 (92.5%) | [80.1%; 97.4%] | 37/40 (92.5%) | 40/40 (100.0%) |",
                        "| C | EXTERNO | 12/18 (66.7%) | [43.7%; 83.7%] | 12/20 (60.0%) | 18/20 (90.0%) |");
    }

    @Test
    void vereditoNosLimiaresDoAdendo7() {
        List<String> linhas = bloco(13, 12).lines().toList();

        assertThat(linhas).contains(
                "| JAVA | 54/55 (98.2%) | 13/16 (81.3%) | menor | 0.0336 | H3 sustentada |",
                "| C | 37/40 (92.5%) | 12/18 (66.7%) | menor | 0.0197 | H3 sustentada |");
    }

    @Test
    void umAcertoAMaisEInconclusivoENuncaRefutado() {
        List<String> linhas = bloco(14, 13).lines().toList();

        assertThat(linhas).contains(
                "| JAVA | 54/55 (98.2%) | 14/16 (87.5%) | menor | 0.1253 | inconclusivo |",
                "| C | 37/40 (92.5%) | 13/18 (72.2%) | menor | 0.0925 | inconclusivo |");
    }

    @Test
    void taxaExternaIgualOuMaiorNaoSustenta() {
        String bloco = bloco(16, 18);

        assertThat(bloco.lines().toList()).contains(
                "| JAVA | 54/55 (98.2%) | 16/16 (100.0%) | maior | 1.0000 | H3 nao sustentada |",
                "| C | 37/40 (92.5%) | 18/18 (100.0%) | maior | 0.5450 | H3 nao sustentada |");
    }

    @Test
    void cadaLinguagemTemOSeuProprioVeredito() {
        List<String> linhas = bloco(13, 16).lines().toList();

        assertThat(linhas).anyMatch(l -> l.startsWith("| JAVA | 54/55") && l.endsWith("| H3 sustentada |"));
        assertThat(linhas).anyMatch(l -> l.startsWith("| C | 37/40") && l.endsWith("| inconclusivo |"));
    }

    @ParameterizedTest(name = "java {0}/16, c {1}/18")
    @CsvSource({"13,12", "14,13", "16,18", "0,0"})
    void oVereditoQueAparecenoTextoEOQueOVereditoH3Devolve(int acertosJava, int acertosC) {
        String bloco = bloco(acertosJava, acertosC);

        assertThat(bloco).contains(rotuloDe(VereditoH3.de(54, 55, acertosJava, 16)))
                .contains(rotuloDe(VereditoH3.de(37, 40, acertosC, 18)));
    }

    private static String rotuloDe(VereditoH3 veredito) {
        return switch (veredito) {
            case SUSTENTADA -> "H3 sustentada";
            case INCONCLUSIVO -> "inconclusivo";
            case NAO_SUSTENTADA -> "H3 nao sustentada";
        };
    }

    @Test
    void inconclusivoNaoTrazRotuloDeSustentadaNemDeNaoSustentada() {
        String bloco = bloco(14, 13);

        assertThat(bloco).doesNotContain("H3 sustentada").doesNotContain("H3 nao sustentada");
    }

    @ParameterizedTest(name = "java {0}/16, c {1}/18")
    @CsvSource({"13,12", "14,13", "16,18"})
    void osLimitesFixosAparecemEmQualquerResultado(int acertosJava, int acertosC) {
        assertThat(bloco(acertosJava, acertosC))
                .contains("dois testes sem correcao para comparacoes multiplas")
                .contains("Baixo poder")
                .contains("a partir de 13/16 em Java e de 12/18 em C")
                .contains("descritivos, sem teste");
    }

    @Test
    void oVereditoUsaABaseDasConstantesENaoOQueOProprioMediu() {
        List<MedidaExterna> externas = new ArrayList<>(MedidasSinteticas.lote(JAVA, 13, 16, 0));
        DadosDoBloco proprioDiferente = new DadosDoBloco(externas, MedidasSinteticas.proprio(40, 55, 37, 40),
                manifesto(), DATA);

        String bloco = RelatorioExterno.renderizar(proprioDiferente);

        assertThat(bloco.lines().toList())
                .contains("| JAVA | 54/55 (98.2%) | 13/16 (81.3%) | menor | 0.0336 | H3 sustentada |");
    }

    @Test
    void denominadorExternoZeroNaoTemVeredito() {
        MedidaExterna soForaDaEscala = respondida(
                caso(JAVA, "001", FORA_DA_ESCALA, FORA_DA_ESCALA, "O(1)", "O(1)", "pior-caso"), "O(n)", "O(1)");

        String bloco = RelatorioExterno.renderizar(dados(List.of(soForaDaEscala)));

        assertThat(bloco).contains("sem veredito: nenhum caso no denominador externo");
    }

    // ---------------------------------------------------------------- recusa, fora da escala, reducao

    @Test
    void recusaFicaNoDenominadorComOMotivoVisivel() {
        List<MedidaExterna> externas = new ArrayList<>();
        externas.add(respondida(caso(JAVA, "001"), "O(n)", "O(1)"));
        externas.add(recusada(caso(JAVA, "002"), MotivoRecusa.FALHA, "ParseProblemException"));
        externas.add(recusada(caso(JAVA, "003"), MotivoRecusa.TEMPO_ESGOTADO, null));
        externas.add(respondida(caso(JAVA, "004"), "?", "O(1)"));
        String bloco = RelatorioExterno.renderizar(dados(externas));

        assertThat(bloco.lines().toList()).contains(
                "| JAVA | EXTERNO | 1/4 (25.0%) | [4.6%; 69.9%] | 1/4 (25.0%) | 4/4 (100.0%) |",
                "| JAVA | 002-002 | tempo | O(n) | O(n) | ? (FALHA: ParseProblemException) | recusou classificar |",
                "| JAVA | 004-004 | tempo | O(n) | O(n) | ? | recusou classificar |");
    }

    @Test
    void divergenciasListamACasaDaRecusaComOMotivo() {
        List<MedidaExterna> externas = List.of(
                recusada(caso(JAVA, "002"), MotivoRecusa.FALHA, "ParseProblemException"),
                recusada(caso(JAVA, "003"), MotivoRecusa.TEMPO_ESGOTADO, null),
                recusada(caso(JAVA, "004"), MotivoRecusa.SEM_METRICA, null));
        String bloco = RelatorioExterno.renderizar(dados(externas));

        assertThat(bloco)
                .contains("| JAVA | 002-002 | tempo | O(n) | O(n) | ? (FALHA: ParseProblemException) | recusou classificar |")
                .contains("| JAVA | 003-003 | tempo | O(n) | O(n) | ? (TEMPO_ESGOTADO) | recusou classificar |")
                .contains("| JAVA | 004-004 | tempo | O(n) | O(n) | ? (SEM_METRICA) | recusou classificar |");
        assertThat(bloco.lines().toList()).contains("| SEM_METRICA | 1 |", "| FALHA | 1 |", "| TEMPO_ESGOTADO | 1 |");
    }

    @Test
    void tempoForaDaEscalaTemTabelaPropriaEFicaForaDoPrincipal() {
        String bloco = bloco(13, 12);

        assertThat(bloco)
                .contains("| JAVA | 200-200 | tempo | O(n*m) | O(n) |")
                .contains("| C | 200-200 | tempo | O(n*m) | O(n) |");
        assertThat(bloco).doesNotContain("Nenhum: todos os gabaritos cabem");
    }

    @Test
    void reducaoAplicadaMostraGabaritoENotaELadoAoMotor() {
        CasoExterno reduzido = caso(C, "315", "O(m log m)", "O(n log n)", "O(m)", "O(n)", "pior-caso");
        String bloco = RelatorioExterno.renderizar(dados(List.of(respondida(reduzido, "O(n log n)", "O(n)"))));

        assertThat(bloco.lines().toList()).contains(
                "| C | 315-315 | tempo | O(m log m) | O(n log n) | O(n log n) | acerto |",
                "| C | 315-315 | espaco | O(m) | O(n) | O(n) | acerto |");
    }

    @Test
    void semReducaoDizQueNenhumaFoiAplicada() {
        String bloco = RelatorioExterno.renderizar(dados(MedidasSinteticas.lote(JAVA, 2, 2, 0)));

        assertThat(bloco).contains("Nenhuma reducao aplicada neste conjunto.");
    }

    @Test
    void tempoEsperadoVaiMarcadoNaListaDeDivergencias() {
        CasoExterno esperado = caso(JAVA, "085", "O(n)", "O(n)", "O(1)", "O(1)", "esperado");
        String bloco = RelatorioExterno.renderizar(dados(List.of(respondida(esperado, "O(n^2)", "O(1)"))));

        assertThat(bloco)
                .contains("| JAVA | 085-085 | tempo (tempo esperado) | O(n) | O(n) | O(n^2) | superestimou |")
                .contains("- JAVA: 1 caso(s) de tempo esperado, 0/1 (0.0%) no denominador principal.");
    }

    // ---------------------------------------------------------------- direcao, confianca, espaco

    @Test
    void direcaoDoErroLadoALado() {
        List<String> linhas = bloco(13, 12).lines().toList();

        assertThat(linhas).contains(
                "| Direcao | JAVA proprio | JAVA EXTERNO | C proprio | C EXTERNO |",
                "| acerto | 54 | 13 | 37 | 12 |",
                "| superestimou | 1 | 3 | 3 | 6 |",
                "| fora da escala | 0 | 4 | 0 | 2 |");
    }

    @Test
    void confiancaEEspacoSaoDescritivos() {
        String bloco = bloco(13, 12);

        assertThat(bloco).contains("### Espaco (descritivo, sem teste)")
                .contains("### Acerto por confianca declarada (tempo, descritivo)");
        assertThat(bloco.lines().toList()).contains("| JAVA | EXTERNO | MEDIA | 13/16 (81.3%) |",
                "| JAVA | proprio | ALTA | 54/55 (98.2%) |");
    }

    // ---------------------------------------------------------------- declaracoes e determinismo

    @Test
    void asDeclaracoesFixasDoProtocoloAparecem() {
        String bloco = bloco(13, 12);

        assertThat(bloco)
                .startsWith("## Categoria EXTERNO\n")
                .contains("`main@5204882`")
                .contains("commit `a70adca`")
                .contains("Adendos 6 e 7 do criterio do corpus externo")
                .contains("so de LeetCode")
                .contains("leetcode (40)")
                .contains("anotado por modelos de linguagem")
                .contains("nao humano")
                .contains("20 casos por linguagem")
                .contains("JAVA 20, C 20")
                .contains("60 s de analise");
    }

    @Test
    void asContagensDeExclusaoVemDoManifestoEmOrdemFixa() {
        String bloco = bloco(13, 12);

        assertThat(bloco)
                .contains("  - JAVA: 135 percorridos, 20 aceitos; exclusoes: divergencia 32, sem-solucao 12")
                .contains("  - C: 535 percorridos, 20 aceitos; exclusoes: divergencia 80, inelegivel 42");
    }

    @Test
    void mesmoInsumoGeraOMesmoTextoQualquerQueSejaOLocalDoSistema() {
        Locale anterior = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("pt-BR"));
            String emPortugues = bloco(13, 12);
            Locale.setDefault(Locale.US);

            assertThat(bloco(13, 12)).isEqualTo(emPortugues).contains("81.3%").doesNotContain("81,3%");
        } finally {
            Locale.setDefault(anterior);
        }
    }

    @Test
    void oBlocoNaoTraiMarcadoresNemCodigo() {
        String bloco = bloco(13, 12);

        assertThat(bloco).doesNotContain(BlocosDoRelatorio.INICIO).doesNotContain(BlocosDoRelatorio.FIM)
                .doesNotContain(CorpusExternoSintetico.TOKEN);
    }

    // ---------------------------------------------------------------- o proprio nao muda

    @Test
    void oExternoNaoEntraNoRelatorioDoProprio() {
        List<Medida> proprias = MedidasSinteticas.proprio(54, 55, 37, 40);

        String proprio = RelatorioDeAcuracia.renderizar(proprias, DATA);

        assertThat(Categoria.values()).extracting(Enum::name).doesNotContain("EXTERNO");
        assertThat(proprio).doesNotContain(BlocosDoRelatorio.INICIO).doesNotContain(BlocosDoRelatorio.FIM)
                .doesNotContain("| EXTERNO |").doesNotContain("`EXTERNO`");
        assertThat(proprio.lines().toList()).contains("| **JAVA** | 55 | 54/55 (98.2%) | 55/55 (100.0%) |",
                "| **C** | 40 | 37/40 (92.5%) | 40/40 (100.0%) |");
    }

    @Test
    void oCabecalhoDoProprioApontaParaASecaoExternaSemAfirmarResultado() {
        String proprio = RelatorioDeAcuracia.renderizar(MedidasSinteticas.proprio(54, 55, 37, 40), DATA);

        assertThat(proprio).contains("Corpus proprio: 95 casos.")
                .contains("secao **Categoria EXTERNO**")
                .doesNotContain("H3 sustentada").doesNotContain("inconclusivo");
    }
}
