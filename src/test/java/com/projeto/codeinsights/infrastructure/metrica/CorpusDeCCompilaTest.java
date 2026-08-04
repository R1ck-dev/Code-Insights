package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

/**
 * Confere que todo caso do corpus de C e C valido — que um compilador de verdade aceita o arquivo.
 * <p>
 * O motor le C por forma, sem parse completo da linguagem: ele nao recusa codigo invalido, e por
 * isso um caso de corpus com erro de sintaxe <b>nao falharia nenhum teste</b>. Ele passaria a ser
 * medido normalmente, e a acuracia publicada no relatorio incluiria a resposta do motor para um
 * arquivo que nao e C. O gabarito seria anotacao humana sobre codigo que nunca compilou.
 * <p>
 * <b>Pula quando nao ha gcc.</b> O compilador nao e dependencia de build: em CI, no Render ou em
 * outra maquina o teste se marca como <i>skipped</i> em vez de quebrar a compilacao do projeto.
 * A checagem so acontece onde ha compilador instalado — que e onde o corpus costuma crescer.
 */
class CorpusDeCCompilaTest {

    /** Resolvido uma vez: sondar o PATH a cada um dos casos so somaria processos. */
    private static final boolean GCC_DISPONIVEL = gccResponde();

    private static final int LIMITE_SEGUNDOS = 60;

    @TempDir
    Path pasta;

    static List<CasoDeCorpus> casosEmC() {
        return CorpusDeAlgoritmos.casosDe(LinguagemProgramacao.C);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosEmC")
    void cadaCasoDoCorpusEhCValido(CasoDeCorpus caso) throws Exception {
        assumeTrue(GCC_DISPONIVEL,
                "gcc nao esta no PATH: a validade sintatica do corpus de C nao foi conferida nesta maquina");

        Path arquivo = pasta.resolve(nomeDeArquivo(caso));
        Files.writeString(arquivo, caso.codigo(), StandardCharsets.UTF_8);

        // -fsyntax-only, e nao build completo: interessa o arquivo ser C valido, nao gerar binario.
        Execucao execucao = rodar("gcc", "-fsyntax-only", "-std=c11", arquivo.toString());

        assertThat(execucao.codigo())
                .as("o gcc recusou %s:%n%s", caso.arquivo(), execucao.saida())
                .isZero();
    }

    /** O gcc reporta o caminho nas mensagens de erro; um nome util faz a falha se explicar sozinha. */
    private static String nomeDeArquivo(CasoDeCorpus caso) {
        String caminho = caso.arquivo();
        return caminho.substring(caminho.lastIndexOf('/') + 1);
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
