package com.projeto.codeinsights.infrastructure.metrica;

import static com.projeto.codeinsights.infrastructure.metrica.BlocosDoRelatorio.FIM;
import static com.projeto.codeinsights.infrastructure.metrica.BlocosDoRelatorio.INICIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * As duas regioes do relatorio: o proprio e regenerado a cada suite sem encostar no bloco externo, e
 * qualquer marcador truncado falha sem tocar no arquivo (que nao esta no git).
 */
class BlocosDoRelatorioTest {

    private static final String PROPRIO_ANTIGO = "# Acuracia\n\nlinha antiga do proprio\n\n";
    private static final String PROPRIO_NOVO = "# Acuracia\n\nlinha NOVA do proprio\n\n";

    /** Corpo com o que mais dificulta a copia byte a byte: CRLF, espaco no fim da linha, acento e simbolos. */
    private static final String CORPO_MEDIDO = "## Categoria EXTERNO\r\n\r\nmedido  \r\n| a | b |\n\tação — fim\n";

    @TempDir
    Path pasta;

    private Path relatorio() {
        return pasta.resolve("notas").resolve("acuracia.md");
    }

    private void gravarBruto(String conteudo) throws IOException {
        Files.createDirectories(relatorio().getParent());
        Files.writeString(relatorio(), conteudo, StandardCharsets.UTF_8);
    }

    private byte[] bytes() throws IOException {
        return Files.readAllBytes(relatorio());
    }

    private static String arquivoComBloco(String proprio, String corpo) {
        return proprio + INICIO + "\n" + corpo + FIM + "\n";
    }

    // ---------------------------------------------------------------- sem bloco

    @Test
    void semArquivoGravaOProprioMaisOAvisoDeNaoMedido() throws IOException {
        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO);

        String conteudo = Files.readString(relatorio(), StandardCharsets.UTF_8);
        assertThat(conteudo).startsWith(PROPRIO_NOVO + INICIO + "\n")
                .contains("externo: nao medido")
                .endsWith(FIM + "\n");
    }

    @Test
    void arquivoAntigoSemMarcadoresGanhaOAvisoSemPerderOProprioNovo() throws IOException {
        gravarBruto("# versao anterior, sem a secao externa\n");

        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO);

        assertThat(Files.readString(relatorio(), StandardCharsets.UTF_8))
                .startsWith(PROPRIO_NOVO).contains("externo: nao medido").doesNotContain("versao anterior");
    }

    // ---------------------------------------------------------------- bloco preservado

    @Test
    void blocoMedidoSobreviveAoProprioRegeneradoByteAByte() throws IOException {
        gravarBruto(arquivoComBloco(PROPRIO_ANTIGO, CORPO_MEDIDO));

        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO);

        byte[] esperado = arquivoComBloco(PROPRIO_NOVO, CORPO_MEDIDO).getBytes(StandardCharsets.UTF_8);
        assertThat(bytes()).isEqualTo(esperado);
    }

    @Test
    void regenerarDuasVezesComOMesmoProprioNaoMudaNenhumByte() throws IOException {
        gravarBruto(arquivoComBloco(PROPRIO_ANTIGO, CORPO_MEDIDO));
        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO);
        byte[] primeira = bytes();

        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO);

        assertThat(bytes()).isEqualTo(primeira);
    }

    @Test
    void oAvisoDeNaoMedidoTambemFicaEstavel() throws IOException {
        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO);
        byte[] primeira = bytes();

        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO);

        assertThat(bytes()).isEqualTo(primeira);
    }

    @Test
    void oProprioNaoInvadeOBlocoNemOBlocoOProprio() throws IOException {
        gravarBruto(arquivoComBloco(PROPRIO_ANTIGO, CORPO_MEDIDO));

        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO);
        String depoisDoProprio = Files.readString(relatorio(), StandardCharsets.UTF_8);
        BlocosDoRelatorio.substituirExterno(relatorio(), "## Categoria EXTERNO\n\nsegunda medicao\n");
        String depoisDoExterno = Files.readString(relatorio(), StandardCharsets.UTF_8);

        assertThat(depoisDoProprio).startsWith(PROPRIO_NOVO).contains(CORPO_MEDIDO);
        assertThat(depoisDoExterno).startsWith(PROPRIO_NOVO).contains("segunda medicao").doesNotContain("medido  ");
        assertThat(depoisDoExterno).endsWith(FIM + "\n");
    }

    @Test
    void substituirExternoTrocaOAvisoPeloBlocoMedido() throws IOException {
        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO);

        BlocosDoRelatorio.substituirExterno(relatorio(), CORPO_MEDIDO);

        assertThat(bytes()).isEqualTo(arquivoComBloco(PROPRIO_NOVO, CORPO_MEDIDO).getBytes(StandardCharsets.UTF_8));
        assertThat(Files.readString(relatorio(), StandardCharsets.UTF_8)).doesNotContain("externo: nao medido");
    }

    @Test
    void substituirExternoEmArquivoSemBlocoAcrescentaNoFim() throws IOException {
        gravarBruto(PROPRIO_ANTIGO);

        BlocosDoRelatorio.substituirExterno(relatorio(), CORPO_MEDIDO);

        assertThat(bytes()).isEqualTo(arquivoComBloco(PROPRIO_ANTIGO, CORPO_MEDIDO).getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------- marcador truncado

    @Test
    void marcadorDeInicioSemFimFalhaSemTocarNoArquivo() throws IOException {
        assertTruncadoNaoToca(PROPRIO_ANTIGO + INICIO + "\n" + CORPO_MEDIDO);
    }

    @Test
    void marcadorDeFimSemInicioFalhaSemTocarNoArquivo() throws IOException {
        assertTruncadoNaoToca(PROPRIO_ANTIGO + CORPO_MEDIDO + FIM + "\n");
    }

    @Test
    void marcadoresForaDeOrdemFalhamSemTocarNoArquivo() throws IOException {
        assertTruncadoNaoToca(PROPRIO_ANTIGO + FIM + "\n" + CORPO_MEDIDO + INICIO + "\n");
    }

    @Test
    void marcadorRepetidoFalhaSemTocarNoArquivo() throws IOException {
        assertTruncadoNaoToca(arquivoComBloco(PROPRIO_ANTIGO, CORPO_MEDIDO) + INICIO + "\n");
        assertTruncadoNaoToca(arquivoComBloco(PROPRIO_ANTIGO, CORPO_MEDIDO) + FIM + "\n");
    }

    private void assertTruncadoNaoToca(String conteudo) throws IOException {
        gravarBruto(conteudo);
        byte[] antes = bytes();

        assertThatThrownBy(() -> BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("nao foi alterado");
        assertThatThrownBy(() -> BlocosDoRelatorio.substituirExterno(relatorio(), CORPO_MEDIDO))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("nao foi alterado");
        assertThatThrownBy(() -> BlocosDoRelatorio.exigirSubstituivel(relatorio()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(bytes()).as("o arquivo continua exatamente como estava").isEqualTo(antes);
    }

    // ---------------------------------------------------------------- outras recusas

    @Test
    void proprioOuBlocoComMarcadorNaoSaoAceitosENaoTocamNoArquivo() throws IOException {
        gravarBruto(arquivoComBloco(PROPRIO_ANTIGO, CORPO_MEDIDO));
        byte[] antes = bytes();

        assertThatThrownBy(() -> BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO + INICIO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BlocosDoRelatorio.substituirExterno(relatorio(), "corpo\n" + FIM))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(bytes()).isEqualTo(antes);
    }

    @Test
    void substituirExternoSemRelatorioFalhaSemCriarOArquivo() {
        assertThatThrownBy(() -> BlocosDoRelatorio.substituirExterno(relatorio(), CORPO_MEDIDO))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("nao existe");
        assertThatThrownBy(() -> BlocosDoRelatorio.exigirSubstituivel(relatorio()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(relatorio()).doesNotExist();
    }

    @Test
    void arquivoQueNaoEUtf8FalhaSemSerReescrito() throws IOException {
        Files.createDirectories(relatorio().getParent());
        Files.write(relatorio(), new byte[] {(byte) 0xC3, (byte) 0x28, '\n'});
        byte[] antes = bytes();

        assertThatThrownBy(() -> BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO))
                .isInstanceOf(java.io.UncheckedIOException.class);

        assertThat(bytes()).isEqualTo(antes);
    }
}
