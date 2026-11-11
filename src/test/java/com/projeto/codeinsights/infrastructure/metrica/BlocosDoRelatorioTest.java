package com.projeto.codeinsights.infrastructure.metrica;

import static com.projeto.codeinsights.infrastructure.metrica.BlocosDoRelatorio.FIM;
import static com.projeto.codeinsights.infrastructure.metrica.BlocosDoRelatorio.INICIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.projeto.codeinsights.infrastructure.metrica.BlocosDoRelatorio.Movedor;

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
                .isInstanceOf(UncheckedIOException.class);

        assertThat(bytes()).isEqualTo(antes);
    }

    // ---------------------------------------------------------------- gravacao sem truncar o original (R1)

    private static final Movedor QUE_FALHA = (origem, destino, opcoes) -> {
        throw new IOException("falha simulada ao mover");
    };

    private List<String> nomesNaPasta() throws IOException {
        try (Stream<Path> arquivos = Files.list(relatorio().getParent())) {
            return arquivos.map(a -> a.getFileName().toString()).sorted().toList();
        }
    }

    private static String capturarSaida(Runnable acao) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(saida, true, StandardCharsets.UTF_8));
            acao.run();
        } finally {
            System.setOut(original);
        }
        return saida.toString(StandardCharsets.UTF_8);
    }

    @Test
    void falhaAoGravarDeixaOOriginalIntactoESemTemporarioEComOBlocoNaSaida() throws IOException {
        gravarBruto(arquivoComBloco(PROPRIO_ANTIGO, CORPO_MEDIDO));
        byte[] antes = bytes();
        List<Throwable> falha = new ArrayList<>();

        String saida = capturarSaida(() -> {
            try {
                BlocosDoRelatorio.substituirExterno(relatorio(), "## Categoria EXTERNO\n\nnovo\n", QUE_FALHA);
            } catch (UncheckedIOException e) {
                falha.add(e);
            }
        });

        assertThat(falha).hasSize(1);
        assertThat(falha.get(0)).hasMessageContaining("original ficou como estava");
        assertThat(bytes()).as("o original continua byte a byte").isEqualTo(antes);
        assertThat(nomesNaPasta()).as("o temporario some").containsExactly("acuracia.md");
        assertThat(saida).as("o bloco ja estava na saida quando a gravacao falhou")
                .contains("## Categoria EXTERNO").contains("novo");
    }

    @Test
    void falhaAoGravarOProprioTambemNaoTocaNoOriginal() throws IOException {
        gravarBruto(arquivoComBloco(PROPRIO_ANTIGO, CORPO_MEDIDO));
        byte[] antes = bytes();

        assertThatThrownBy(() -> BlocosDoRelatorio.gravar(relatorio(), PROPRIO_NOVO, QUE_FALHA))
                .isInstanceOf(UncheckedIOException.class);

        assertThat(bytes()).isEqualTo(antes);
        assertThat(nomesNaPasta()).containsExactly("acuracia.md");
    }

    @Test
    void marcadorTruncadoNaoImprimeNadaEFalhaAntesDeGravar() throws IOException {
        gravarBruto(PROPRIO_ANTIGO + INICIO + "\n" + CORPO_MEDIDO);
        byte[] antes = bytes();

        String saida = capturarSaida(() -> assertThatThrownBy(
                () -> BlocosDoRelatorio.substituirExterno(relatorio(), "corpo\n", QUE_FALHA))
                .isInstanceOf(IllegalStateException.class));

        assertThat(saida).doesNotContain("corpo");
        assertThat(bytes()).isEqualTo(antes);
    }

    @Test
    void movimentoAtomicoEOPrimeiroEOUnicoQuandoFunciona() throws IOException {
        gravarBruto(arquivoComBloco(PROPRIO_ANTIGO, CORPO_MEDIDO));
        List<List<CopyOption>> chamadas = new ArrayList<>();
        Movedor registrador = (origem, destino, opcoes) -> {
            chamadas.add(List.of(opcoes));
            Files.move(origem, destino, opcoes);
        };

        BlocosDoRelatorio.gravar(relatorio(), PROPRIO_NOVO, registrador);

        assertThat(chamadas).containsExactly(List.of(StandardCopyOption.ATOMIC_MOVE));
        assertThat(Files.readString(relatorio(), StandardCharsets.UTF_8)).isEqualTo(PROPRIO_NOVO);
        assertThat(nomesNaPasta()).containsExactly("acuracia.md");
    }

    @Test
    void semMovimentoAtomicoCaiParaSubstituirPorCima() throws IOException {
        gravarBruto(arquivoComBloco(PROPRIO_ANTIGO, CORPO_MEDIDO));
        List<List<CopyOption>> chamadas = new ArrayList<>();
        Movedor semAtomico = (origem, destino, opcoes) -> {
            chamadas.add(List.of(opcoes));
            if (List.of(opcoes).contains(StandardCopyOption.ATOMIC_MOVE)) {
                throw new AtomicMoveNotSupportedException(origem.toString(), destino.toString(), "simulado");
            }
            Files.move(origem, destino, opcoes);
        };

        BlocosDoRelatorio.gravar(relatorio(), PROPRIO_NOVO, semAtomico);

        assertThat(chamadas).containsExactly(List.of(StandardCopyOption.ATOMIC_MOVE),
                List.of(StandardCopyOption.REPLACE_EXISTING));
        assertThat(Files.readString(relatorio(), StandardCharsets.UTF_8)).isEqualTo(PROPRIO_NOVO);
        assertThat(nomesNaPasta()).containsExactly("acuracia.md");
    }

    @Test
    void seOFallbackTambemFalharOOriginalFicaIntactoESemTemporario() throws IOException {
        gravarBruto(arquivoComBloco(PROPRIO_ANTIGO, CORPO_MEDIDO));
        byte[] antes = bytes();
        Movedor semNada = (origem, destino, opcoes) -> {
            throw new AtomicMoveNotSupportedException(origem.toString(), destino.toString(), "simulado");
        };

        assertThatThrownBy(() -> BlocosDoRelatorio.gravar(relatorio(), PROPRIO_NOVO, semNada))
                .isInstanceOf(UncheckedIOException.class);

        assertThat(bytes()).isEqualTo(antes);
        assertThat(nomesNaPasta()).containsExactly("acuracia.md");
    }

    @Test
    void gravacaoNormalCriaAPastaEDeixaSoOArquivo() throws IOException {
        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO);

        assertThat(nomesNaPasta()).containsExactly("acuracia.md");
    }

    // ---------------------------------------------------------------- deteccao exata do aviso de nao medido (R2)

    @Test
    void arquivoSemBlocoOuComOAvisoExatoContaComoNaoMedido() throws IOException {
        assertThat(BlocosDoRelatorio.externoNaoMedido(PROPRIO_NOVO)).isTrue();

        BlocosDoRelatorio.regenerarProprio(relatorio(), PROPRIO_NOVO);

        assertThat(BlocosDoRelatorio.externoNaoMedido(relatorio())).isTrue();
        assertThat(BlocosDoRelatorio.externoNaoMedido(Files.readString(relatorio(), StandardCharsets.UTF_8))).isTrue();
    }

    @Test
    void blocoMedidoNaoContaComoNaoMedido() {
        assertThat(BlocosDoRelatorio.externoNaoMedido(arquivoComBloco(PROPRIO_NOVO, CORPO_MEDIDO))).isFalse();
    }

    @Test
    void avisoComUmaDiferencaQualquerNaoContaComoNaoMedido() {
        String aviso = BlocosDoRelatorio.NAO_MEDIDO;

        assertThat(BlocosDoRelatorio.externoNaoMedido(arquivoComBloco(PROPRIO_NOVO, aviso + "medido\n"))).isFalse();
        assertThat(BlocosDoRelatorio.externoNaoMedido(
                arquivoComBloco(PROPRIO_NOVO, aviso.replace("nao medido", "medido")))).isFalse();
        assertThat(BlocosDoRelatorio.externoNaoMedido(arquivoComBloco(PROPRIO_NOVO, aviso.trim()))).isFalse();
        assertThat(BlocosDoRelatorio.externoNaoMedido(arquivoComBloco(PROPRIO_NOVO, aviso + "\n"))).isFalse();
        assertThat(BlocosDoRelatorio.externoNaoMedido(arquivoComBloco(PROPRIO_NOVO, ""))).isFalse();
    }

    @Test
    void avisoComQuebraDeLinhaCrlfNaoContaComoNaoMedido() {
        String aviso = BlocosDoRelatorio.NAO_MEDIDO;
        String avisoCrlf = aviso.replace("\n", "\r\n");

        assertThat(BlocosDoRelatorio.externoNaoMedido(PROPRIO_NOVO + INICIO + "\n" + avisoCrlf + FIM + "\n"))
                .as("so o corpo do aviso com CRLF").isFalse();
        assertThat(BlocosDoRelatorio.externoNaoMedido(PROPRIO_NOVO + INICIO + "\r\n" + aviso + FIM + "\r\n"))
                .as("so o marcador de inicio com CRLF").isFalse();
        assertThat(BlocosDoRelatorio.externoNaoMedido(
                (PROPRIO_NOVO + INICIO + "\n" + aviso + FIM + "\n").replace("\n", "\r\n")))
                .as("o arquivo inteiro com CRLF").isFalse();
    }

    @Test
    void marcadorTruncadoFalhaEmVezDeContarComoNaoMedido() {
        assertThatThrownBy(() -> BlocosDoRelatorio.externoNaoMedido(PROPRIO_NOVO + INICIO + "\n"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void relatorioAusenteFalhaEmVezDeContarComoNaoMedido() {
        assertThatThrownBy(() -> BlocosDoRelatorio.externoNaoMedido(relatorio()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("nao existe");
    }
}
