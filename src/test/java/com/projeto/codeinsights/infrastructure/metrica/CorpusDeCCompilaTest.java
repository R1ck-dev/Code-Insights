package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Confere que todo arquivo {@code .c} usado nos testes e C valido — que um compilador de verdade
 * aceita o arquivo.
 * <p>
 * O motor le C por forma, sem parse completo da linguagem: ele nao recusa codigo invalido, e por
 * isso um caso com erro de sintaxe <b>nao falharia nenhum teste</b>. Ele passaria a ser medido
 * normalmente, e a acuracia publicada no relatorio incluiria a resposta do motor para um arquivo
 * que nao e C. O gabarito seria anotacao humana sobre codigo que nunca compilou.
 * <p>
 * Varre {@code src/test/resources} inteiro, e nao so o corpus, porque os pares de
 * {@link EquivalenciaEntreLinguagensTest} correm o mesmo risco — e qualquer conjunto de casos que
 * venha depois entra nesta rede sem precisar lembrar de registra-lo aqui.
 * <p>
 * <b>Pula quando nao ha gcc.</b> O compilador nao e dependencia de build: em CI, no Render ou em
 * outra maquina o teste se marca como <i>skipped</i> em vez de quebrar a compilacao do projeto.
 * A checagem so acontece onde ha compilador instalado — que e onde o corpus costuma crescer.
 */
class CorpusDeCCompilaTest {

    /** Resolvido uma vez: sondar o PATH a cada um dos casos so somaria processos. */
    private static final boolean GCC_DISPONIVEL = gccResponde();

    private static final Path RAIZ = Path.of("src", "test", "resources");

    private static final int LIMITE_SEGUNDOS = 60;

    static List<Path> arquivosEmC() throws IOException {
        try (Stream<Path> arquivos = Files.walk(RAIZ)) {
            return arquivos.filter(caminho -> caminho.toString().endsWith(".c")).sorted().toList();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("arquivosEmC")
    void oArquivoEhCValido(Path arquivo) throws Exception {
        assumeTrue(GCC_DISPONIVEL,
                "gcc nao esta no PATH: a validade sintatica dos arquivos em C nao foi conferida nesta maquina");

        // -fsyntax-only, e nao build completo: interessa o arquivo ser C valido, nao gerar binario.
        // Sem -Wall de proposito — variavel nao usada e estilo, e ha casos deliberadamente ingenuos.
        Execucao execucao = rodar("gcc", "-fsyntax-only", "-std=c11", arquivo.toString());

        assertThat(execucao.codigo())
                .as("o gcc recusou %s:%n%s", arquivo, execucao.saida())
                .isZero();
    }

    private record Execucao(int codigo, String saida) {
    }

    private static Execucao rodar(String... comando) throws IOException, InterruptedException {
        Process processo = new ProcessBuilder(comando).redirectErrorStream(true).start();
        String saida = new String(processo.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!processo.waitFor(LIMITE_SEGUNDOS, TimeUnit.SECONDS)) {
            processo.destroyForcibly();
            throw new IllegalStateException("O comando %s nao terminou em %ds."
                    .formatted(String.join(" ", comando), LIMITE_SEGUNDOS));
        }
        return new Execucao(processo.exitValue(), saida);
    }

    private static boolean gccResponde() {
        try {
            return rodar("gcc", "--version").codigo() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
