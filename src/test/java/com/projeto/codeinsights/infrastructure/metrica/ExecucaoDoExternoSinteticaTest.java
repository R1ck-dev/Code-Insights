package com.projeto.codeinsights.infrastructure.metrica;

import static com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.PADRAO;
import static com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.SEGUNDO_JAVA;
import static com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.infrastructure.metrica.ExecucaoDoExterno.Modo;
import com.projeto.codeinsights.infrastructure.metrica.ExecucaoDoExterno.Resultado;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Medida;

/**
 * O pipeline inteiro sobre fixtures sinteticas: pasta de fontes e relatorio em {@code @TempDir},
 * corpus proprio de mentira (ou o proprio de verdade, uma vez), e nenhum fonte de terceiros. Prova a
 * ordem que protege a primeira execucao: base conferida antes de carregar ou medir o externo, e
 * relatorio intocado quando algo falha.
 */
class ExecucaoDoExternoSinteticaTest {

    private static final LocalDate DATA = LocalDate.of(2026, 11, 6);
    private static final String PROPRIO = "# Acuracia do motor\n\nlinhas do corpus proprio\n\n";

    @TempDir
    Path pasta;

    private Path fontes() {
        return pasta.resolve("fontes");
    }

    private Path relatorio() {
        return pasta.resolve("relatorio").resolve("acuracia.md");
    }

    private static GabaritoExterno gabarito() {
        return GabaritoExterno.de(CorpusExternoSintetico.gabarito(PADRAO));
    }

    private static ManifestoExterno manifesto() {
        return ManifestoExterno.de(CorpusExternoSintetico.manifesto(PADRAO));
    }

    private void prepararFontesERelatorio() {
        CorpusExternoSintetico.gravar(fontes(), PADRAO);
        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO);
    }

    private static Supplier<List<Medida>> proprioNaBase() {
        return () -> MedidasSinteticas.proprio(54, 55, 37, 40);
    }

    private String lerRelatorio() throws IOException {
        return Files.readString(relatorio(), StandardCharsets.UTF_8);
    }

    private Resultado executar(Supplier<List<Medida>> proprio, MedidorExterno medidor) {
        return executar(proprio, medidor, Modo.MEDIR);
    }

    private Resultado executar(Supplier<List<Medida>> proprio, MedidorExterno medidor, Modo modo) {
        return ExecucaoDoExterno.executar(fontes(), gabarito(), manifesto(), relatorio(), proprio, medidor, DATA,
                modo);
    }

    private static MedidorExterno medidorQueConta(AtomicInteger chamadas) {
        return new MedidorExterno(Duration.ofSeconds(60), (codigo, linguagem) -> {
            chamadas.incrementAndGet();
            return MedidorDoCorpus.medir(codigo, linguagem);
        });
    }

    // ---------------------------------------------------------------- caminho feliz

    @Test
    void medeOsTresFontesEGravaOBlocoSemTocarNoProprio() throws IOException {
        prepararFontesERelatorio();
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        PrintStream original = System.out;
        Resultado resultado;
        try {
            System.setOut(new PrintStream(saida, true, StandardCharsets.UTF_8));
            resultado = executar(proprioNaBase(), new MedidorExterno());
        } finally {
            System.setOut(original);
        }

        String conteudo = lerRelatorio();
        assertThat(resultado.medidas()).hasSize(3);
        assertThat(conteudo).startsWith(PROPRIO).contains(BlocosDoRelatorio.INICIO, BlocosDoRelatorio.FIM)
                .contains(resultado.bloco()).contains("## Categoria EXTERNO")
                .doesNotContain("externo: nao medido");
        assertThat(conteudo).contains("Medicao de 2026-11-06");
        assertThat(saida.toString(StandardCharsets.UTF_8)).doesNotContain(TOKEN);
    }

    @Test
    void naoVazaCodigoDeTerceirosNemOTokenParaORelatorioNemParaOResultado() throws IOException {
        prepararFontesERelatorio();

        Resultado resultado = executar(proprioNaBase(), new MedidorExterno());

        assertThat(lerRelatorio()).doesNotContain(TOKEN).doesNotContain("class Soma").doesNotContain("int soma")
                .doesNotContain("ação");
        assertThat(resultado.bloco()).doesNotContain(TOKEN);
        assertThat(resultado.medidas().toString()).doesNotContain(TOKEN);
    }

    @Test
    void oBlocoTraDadosDaAnotacaoENuncaDoFonte() throws IOException {
        prepararFontesERelatorio();

        String bloco = executar(proprioNaBase(), new MedidorExterno()).bloco();

        assertThat(bloco).contains("JAVA 2, C 1").contains("leetcode (3)");
    }

    @Test
    void oBlocoVaiParaASaidaPadraoAntesDeGravar() throws IOException {
        prepararFontesERelatorio();
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        PrintStream original = System.out;
        Resultado resultado;
        try {
            System.setOut(new PrintStream(saida, true, StandardCharsets.UTF_8));
            resultado = executar(proprioNaBase(), new MedidorExterno());
        } finally {
            System.setOut(original);
        }

        assertThat(saida.toString(StandardCharsets.UTF_8)).contains(resultado.bloco());
    }

    @Test
    void remedirExplicitamenteSubstituiOBlocoEMantemOProprio() throws IOException {
        prepararFontesERelatorio();
        executar(proprioNaBase(), new MedidorExterno());
        String primeira = lerRelatorio();

        executar(proprioNaBase(), new MedidorExterno(), Modo.REMEDIR);

        assertThat(lerRelatorio()).isEqualTo(primeira);
        assertThat(primeira.indexOf(BlocosDoRelatorio.INICIO)).isEqualTo(primeira.lastIndexOf(BlocosDoRelatorio.INICIO));
    }

    @Test
    void medirDeNovoSobreUmBlocoMedidoRecusaSemMedirNemTocarNosFontesNemApagarNada() throws IOException {
        prepararFontesERelatorio();
        executar(proprioNaBase(), new MedidorExterno());
        byte[] medido = Files.readAllBytes(relatorio());
        AtomicInteger proprioChamado = new AtomicInteger();
        AtomicInteger chamadas = new AtomicInteger();
        ManifestoExterno hashErrado = ManifestoExterno.de(
                CorpusExternoSintetico.manifesto(PADRAO, Map.of(SEGUNDO_JAVA.nome(), "0".repeat(64))));

        assertThatThrownBy(() -> ExecucaoDoExterno.executar(pasta.resolve("nao-existe"), gabarito(), hashErrado,
                relatorio(), () -> {
                    proprioChamado.incrementAndGet();
                    return MedidasSinteticas.proprio(54, 55, 37, 40);
                }, medidorQueConta(chamadas), DATA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("medicao e unica")
                .hasMessageContaining("Adendo 6, item 9")
                .hasMessageContaining("corpus.externo=remedir")
                .hasMessageNotContaining("Pasta da linguagem ausente")
                .hasMessageNotContaining("SHA-256");

        assertThat(proprioChamado).as("nao mediu o proprio").hasValue(0);
        assertThat(chamadas).as("nao mediu o externo").hasValue(0);
        assertThat(Files.readAllBytes(relatorio())).as("nada foi apagado").isEqualTo(medido);
    }

    @Test
    void medirSobreOAvisoDeNaoMedidoNaoPrecisaDePermissao() throws IOException {
        prepararFontesERelatorio();
        assertThat(lerRelatorio()).contains("externo: nao medido");

        Resultado resultado = executar(proprioNaBase(), new MedidorExterno());

        assertThat(resultado.medidas()).hasSize(3);
        assertThat(lerRelatorio()).doesNotContain("externo: nao medido");
    }

    @Test
    void relatorioSemBlocoTambemContaComoNaoMedido() throws IOException {
        CorpusExternoSintetico.gravar(fontes(), PADRAO);
        Files.createDirectories(relatorio().getParent());
        Files.writeString(relatorio(), PROPRIO, StandardCharsets.UTF_8);

        Resultado resultado = executar(proprioNaBase(), new MedidorExterno());

        assertThat(resultado.medidas()).hasSize(3);
        assertThat(lerRelatorio()).startsWith(PROPRIO).contains(resultado.bloco());
    }

    @Test
    void avisoComCrlfNaoEReconhecidoERecusaMedirMasRemedirExplicitoDeixaPassar() throws IOException {
        CorpusExternoSintetico.gravar(fontes(), PADRAO);
        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO);
        String comCrlf = lerRelatorio().replace("\n", "\r\n");
        Files.writeString(relatorio(), comCrlf, StandardCharsets.UTF_8);
        byte[] antes = Files.readAllBytes(relatorio());
        AtomicInteger proprioChamado = new AtomicInteger();

        assertThatThrownBy(() -> executar(() -> {
            proprioChamado.incrementAndGet();
            return MedidasSinteticas.proprio(54, 55, 37, 40);
        }, new MedidorExterno())).isInstanceOf(IllegalStateException.class).hasMessageContaining("medicao e unica");

        assertThat(proprioChamado).hasValue(0);
        assertThat(Files.readAllBytes(relatorio())).isEqualTo(antes);
        assertThat(executar(proprioNaBase(), new MedidorExterno(), Modo.REMEDIR).medidas()).hasSize(3);
    }

    @Test
    void remedirNaoPermiteMarcadorTruncado() throws IOException {
        CorpusExternoSintetico.gravar(fontes(), PADRAO);
        Files.createDirectories(relatorio().getParent());
        Files.writeString(relatorio(), PROPRIO + BlocosDoRelatorio.INICIO + "\nincompleto\n", StandardCharsets.UTF_8);
        byte[] antes = Files.readAllBytes(relatorio());

        assertThatThrownBy(() -> executar(proprioNaBase(), new MedidorExterno(), Modo.REMEDIR))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("truncados");

        assertThat(Files.readAllBytes(relatorio())).isEqualTo(antes);
    }

    @Test
    void comOProprioDeVerdadeABaseDeIc13Confere() throws IOException {
        prepararFontesERelatorio();

        Resultado resultado = ExecucaoDoExterno.executar(fontes(), gabarito(), manifesto(), relatorio());

        assertThat(resultado.medidas()).hasSize(3);
        assertThat(lerRelatorio()).contains("| JAVA | proprio | 54/55 (98.2%) |")
                .contains("| C | proprio | 37/40 (92.5%) |");
    }

    // ---------------------------------------------------------------- a ordem que protege a primeira execucao

    @Test
    void baseDiferenteAbortaAntesDeCarregarOuMedirEDeixaORelatorioComoEstava() throws IOException {
        CorpusExternoSintetico.gravar(fontes(), PADRAO);
        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO);
        byte[] antes = Files.readAllBytes(relatorio());
        AtomicInteger chamadas = new AtomicInteger();
        ManifestoExterno hashErrado = ManifestoExterno.de(
                CorpusExternoSintetico.manifesto(PADRAO, Map.of(SEGUNDO_JAVA.nome(), "0".repeat(64))));

        assertThatThrownBy(() -> ExecucaoDoExterno.executar(fontes(), gabarito(), hashErrado, relatorio(),
                () -> MedidasSinteticas.proprio(53, 55, 37, 40), medidorQueConta(chamadas), DATA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JAVA: medido 53/55, base 54/55")
                .hasMessageContaining("nao foi tocado")
                .hasMessageNotContaining("SHA-256")
                .hasMessageNotContaining(TOKEN);

        assertThat(chamadas).hasValue(0);
        assertThat(Files.readAllBytes(relatorio())).isEqualTo(antes);
    }

    @Test
    void baseDiferenteAbortaMesmoComAPastaDeFontesAusente() throws IOException {
        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO);
        AtomicInteger chamadas = new AtomicInteger();

        assertThatThrownBy(() -> ExecucaoDoExterno.executar(pasta.resolve("nao-existe"), gabarito(), manifesto(),
                relatorio(), () -> MedidasSinteticas.proprio(54, 55, 36, 40), medidorQueConta(chamadas), DATA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("C: medido 36/40, base 37/40")
                .hasMessageNotContaining("Pasta da linguagem ausente");

        assertThat(chamadas).hasValue(0);
    }

    @Test
    void baseComDenominadorDiferenteTambemAborta() throws IOException {
        prepararFontesERelatorio();

        assertThatThrownBy(() -> executar(() -> MedidasSinteticas.proprio(54, 56, 37, 40), new MedidorExterno()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JAVA: medido 54/56, base 54/55");
    }

    @Test
    void marcadorTruncadoAbortaAntesDeMedirOProprio() throws IOException {
        CorpusExternoSintetico.gravar(fontes(), PADRAO);
        Files.createDirectories(relatorio().getParent());
        Files.writeString(relatorio(), PROPRIO + BlocosDoRelatorio.INICIO + "\nincompleto\n", StandardCharsets.UTF_8);
        byte[] antes = Files.readAllBytes(relatorio());
        AtomicInteger proprioChamado = new AtomicInteger();

        assertThatThrownBy(() -> executar(() -> {
            proprioChamado.incrementAndGet();
            return MedidasSinteticas.proprio(54, 55, 37, 40);
        }, new MedidorExterno())).isInstanceOf(IllegalStateException.class).hasMessageContaining("truncados");

        assertThat(proprioChamado).hasValue(0);
        assertThat(Files.readAllBytes(relatorio())).isEqualTo(antes);
    }

    @Test
    void relatorioAusenteAbortaAntesDeMedirOProprio() {
        CorpusExternoSintetico.gravar(fontes(), PADRAO);
        AtomicInteger proprioChamado = new AtomicInteger();

        assertThatThrownBy(() -> executar(() -> {
            proprioChamado.incrementAndGet();
            return MedidasSinteticas.proprio(54, 55, 37, 40);
        }, new MedidorExterno())).isInstanceOf(IllegalStateException.class).hasMessageContaining("nao existe");

        assertThat(proprioChamado).hasValue(0);
        assertThat(relatorio()).doesNotExist();
    }

    @Test
    void hashDoFonteErradoAbortaAntesDeMedirESemGravarONaoMedido() throws IOException {
        prepararFontesERelatorio();
        byte[] antes = Files.readAllBytes(relatorio());
        AtomicInteger chamadas = new AtomicInteger();
        ManifestoExterno hashErrado = ManifestoExterno.de(
                CorpusExternoSintetico.manifesto(PADRAO, Map.of(SEGUNDO_JAVA.nome(), "0".repeat(64))));

        assertThatThrownBy(() -> ExecucaoDoExterno.executar(fontes(), gabarito(), hashErrado, relatorio(),
                proprioNaBase(), medidorQueConta(chamadas), DATA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SHA-256 diferente do manifesto")
                .hasMessageContaining(SEGUNDO_JAVA.nome())
                .hasMessageNotContaining(TOKEN);

        assertThat(chamadas).hasValue(0);
        assertThat(Files.readAllBytes(relatorio())).isEqualTo(antes);
    }

    @Test
    void recusaDeUmCasoNaoImpedeAGravacaoDoBloco() throws IOException {
        prepararFontesERelatorio();
        MedidorExterno falhaEmC = new MedidorExterno(Duration.ofSeconds(60), (codigo, linguagem) -> {
            if (linguagem == LinguagemProgramacao.C) {
                throw new IllegalArgumentException("trecho do fonte: " + TOKEN);
            }
            return MedidorDoCorpus.medir(codigo, linguagem);
        });

        Resultado resultado = executar(proprioNaBase(), falhaEmC);

        assertThat(resultado.medidas()).hasSize(3);
        assertThat(lerRelatorio()).contains("? (FALHA: IllegalArgumentException)").doesNotContain(TOKEN);
    }
}
