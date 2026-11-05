package com.projeto.codeinsights.infrastructure.metrica;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * As duas regioes de {@code acuracia-do-motor.md}: a parte do corpus proprio, que o
 * {@code RelatorioDeAcuraciaTest} regenera a cada suite, e a secao {@code Categoria EXTERNO}, entre
 * {@link #INICIO} e {@link #FIM}, que so a medicao do externo escreve.
 * <p>
 * O arquivo nao esta no git, entao um erro aqui perde a medicao. Por isso cada gravacao monta a
 * string inteira antes de abrir o arquivo, e um marcador truncado, repetido ou fora de ordem falha
 * sem tocar nele. A regiao do bloco, do primeiro marcador ao ultimo, e copiada como esta: byte a byte.
 * <p>
 * A gravacao nao trunca o original: o texto vai para um temporario na mesma pasta e so entao o
 * temporario e movido por cima do arquivo (atomico quando o sistema de arquivos deixa). Se qualquer
 * passo falha, o original fica como estava e o temporario some.
 */
final class BlocosDoRelatorio {

    static final String INICIO = "<!-- EXTERNO:INICIO -->";
    static final String FIM = "<!-- EXTERNO:FIM -->";

    /** O que o arquivo diz enquanto o corpus externo nao foi medido. */
    static final String NAO_MEDIDO = "## Categoria EXTERNO\n\n"
            + "**externo: nao medido.** Esta secao e preenchida pela medicao unica do corpus externo "
            + "(Adendo 6, item 9). Enquanto ela nao rodar, todos os numeros deste arquivo sao do corpus proprio.\n";

    /** Como o temporario vira o arquivo; e o que o teste troca para simular uma falha ao gravar. */
    @FunctionalInterface
    interface Movedor {
        void mover(Path origem, Path destino, CopyOption... opcoes) throws IOException;
    }

    static final Movedor DO_SISTEMA = Files::move;

    private BlocosDoRelatorio() {
    }

    // ---------------------------------------------------------------- composicao (sem arquivo)

    /**
     * O arquivo com a parte do proprio trocada por {@code proprio} e o bloco que ja existia, intacto.
     * {@code existente} e {@code null} quando o arquivo ainda nao existe: entra o aviso de nao medido.
     */
    static String comProprio(String existente, String proprio) {
        exigirSemMarcador(proprio, "A parte do corpus proprio");
        return proprio + (existente == null ? envolver(NAO_MEDIDO) : regiaoOuNaoMedido(existente)) + "\n";
    }

    /**
     * O arquivo com o bloco externo trocado por {@code corpo}, e a parte do proprio intacta. Sem bloco
     * no arquivo, o novo entra no fim.
     */
    static String comExterno(String existente, String corpo) {
        exigirSemMarcador(corpo, "O corpo do bloco externo");
        int inicio = validar(existente);
        String proprio = inicio < 0 ? existente : existente.substring(0, inicio);
        return proprio + envolver(corpo) + "\n";
    }

    /** Falha se o arquivo tem marcador truncado, repetido ou fora de ordem; nao altera nada. */
    static void exigirBlocoValido(String existente) {
        validar(existente);
    }

    // ---------------------------------------------------------------- arquivo

    /** Regenera a parte do proprio e preserva o bloco externo; cria o arquivo se ele nao existe. */
    static void regenerarProprio(Path arquivo, String proprio) {
        String novo = comProprio(lerOuNulo(arquivo), proprio);
        gravar(arquivo, novo, DO_SISTEMA);
    }

    /**
     * Troca so o bloco externo. O arquivo tem de existir: a parte do proprio e quem o cria. O corpo vai
     * para a saida padrao <b>antes</b> de gravar, para que uma falha na gravacao nao perca a medicao.
     */
    static void substituirExterno(Path arquivo, String corpo) {
        substituirExterno(arquivo, corpo, DO_SISTEMA);
    }

    static void substituirExterno(Path arquivo, String corpo, Movedor movedor) {
        String novo = comExterno(lerExistente(arquivo), corpo);
        System.out.println(corpo);
        gravar(arquivo, novo, movedor);
    }

    /**
     * Verdadeiro se o arquivo nao tem bloco externo ou se o bloco e <b>exatamente</b> o aviso de nao
     * medido, byte a byte: qualquer outra coisa (inclusive o aviso com quebra de linha CRLF) conta como
     * medida, porque errar para esse lado so recusa uma medicao, nunca sobrescreve uma.
     */
    static boolean externoNaoMedido(String existente) {
        int inicio = validar(existente);
        return inicio < 0 || regiao(existente, inicio).equals(envolver(NAO_MEDIDO));
    }

    static boolean externoNaoMedido(Path arquivo) {
        return externoNaoMedido(lerExistente(arquivo));
    }

    /** Confere, sem escrever, que o arquivo existe e que a troca do bloco e possivel. */
    static void exigirSubstituivel(Path arquivo) {
        exigirBlocoValido(lerExistente(arquivo));
    }

    private static String lerExistente(Path arquivo) {
        String existente = lerOuNulo(arquivo);
        if (existente == null) {
            throw new IllegalStateException(
                    "O relatorio nao existe; a parte do corpus proprio (RelatorioDeAcuraciaTest) vem antes.");
        }
        return existente;
    }

    static String lerOuNulo(Path arquivo) {
        if (!Files.exists(arquivo)) {
            return null;
        }
        try {
            return Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler o relatorio.", e);
        }
    }

    static void gravar(Path arquivo, String conteudo, Movedor movedor) {
        Path temporario = null;
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            Files.createDirectories(pasta);
            temporario = Files.createTempFile(pasta, "relatorio-", ".tmp");
            Files.writeString(temporario, conteudo, StandardCharsets.UTF_8);
            moverPorCima(movedor, temporario, arquivo);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel gravar o relatorio; o original ficou como estava.", e);
        } finally {
            apagar(temporario);
        }
    }

    /** Move atomico; se o sistema de arquivos nao suporta (pasta sincronizada, por exemplo), troca por cima. */
    private static void moverPorCima(Movedor movedor, Path temporario, Path arquivo) throws IOException {
        try {
            movedor.mover(temporario, arquivo, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            movedor.mover(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void apagar(Path temporario) {
        if (temporario == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporario);
        } catch (IOException e) {
            // o original ja esta a salvo; sobra so um temporario com o prefixo relatorio-
        }
    }

    // ---------------------------------------------------------------- marcadores

    private static String envolver(String corpo) {
        return INICIO + "\n" + corpo + (corpo.endsWith("\n") ? "" : "\n") + FIM;
    }

    private static String regiaoOuNaoMedido(String existente) {
        int inicio = validar(existente);
        return inicio < 0 ? envolver(NAO_MEDIDO) : regiao(existente, inicio);
    }

    private static String regiao(String existente, int inicio) {
        return existente.substring(inicio, existente.indexOf(FIM) + FIM.length());
    }

    /**
     * Posicao do marcador de inicio, ou {@code -1} se o arquivo nao tem bloco. Exatamente um par, na
     * ordem certa; qualquer outra coisa falha.
     */
    private static int validar(String existente) {
        int inicio = existente.indexOf(INICIO);
        int fim = existente.indexOf(FIM);
        boolean unicos = inicio == existente.lastIndexOf(INICIO) && fim == existente.lastIndexOf(FIM);
        if (inicio < 0 && fim < 0) {
            return -1;
        }
        if (inicio < 0 || fim < 0 || !unicos || fim < inicio) {
            throw new IllegalStateException("Marcadores do bloco externo truncados, repetidos ou fora de ordem; "
                    + "o relatorio nao foi alterado. Corrija ou apague o bloco a mao antes de regenerar.");
        }
        return inicio;
    }

    private static void exigirSemMarcador(String texto, String quem) {
        if (texto.contains(INICIO) || texto.contains(FIM)) {
            throw new IllegalArgumentException(quem + " nao pode conter os marcadores do bloco externo.");
        }
    }
}
