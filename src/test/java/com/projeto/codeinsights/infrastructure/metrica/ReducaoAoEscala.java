package com.projeto.codeinsights.infrastructure.metrica;

import java.util.List;

/**
 * A reducao do gabarito a uma classe da escala do motor (Adendo 6, item 2). Um gabarito so e
 * convertido quando uma unica variavel de entrada cresce, depois de tirar o que a restricao do
 * problema torna constante ou dominado. A regra esta fechada em cinco entradas, escritas antes de
 * medir; o gabarito em si nao muda, so a nota.
 * <p>
 * A tabela e uma <b>trava</b>: se o texto do gabarito de um caso da tabela for diferente do que o
 * Adendo registrou, a leitura falha em vez de reduzir um valor que ninguem decidiu. Fora da tabela,
 * a nota e o proprio gabarito.
 */
final class ReducaoAoEscala {

    enum Metrica {
        TEMPO, ESPACO
    }

    /** Uma linha da tabela do Adendo 6, item 2. */
    record Reducao(String arquivo, Metrica metrica, String gabarito, String nota) {
    }

    private static final List<Reducao> TABELA = List.of(
            new Reducao("021-leetcode-3022.java", Metrica.TEMPO, "O(n * B)", "O(n)"),
            new Reducao("049-leetcode-1334.c", Metrica.TEMPO, "O(n^3 + e)", "O(n^3)"),
            new Reducao("315-leetcode-2225.c", Metrica.TEMPO, "O(m log m)", "O(n log n)"),
            new Reducao("315-leetcode-2225.c", Metrica.ESPACO, "O(m)", "O(n)"),
            new Reducao("373-leetcode-392.c", Metrica.TEMPO, "O(m)", "O(n)"));

    private ReducaoAoEscala() {
    }

    static List<Reducao> tabela() {
        return TABELA;
    }

    /**
     * A classe com que o caso e comparado: a nota da tabela, ou o proprio {@code gabarito} quando o
     * caso nao esta nela. Lanca {@link IllegalStateException} se o caso esta na tabela mas o texto do
     * gabarito difere do registrado.
     */
    static String nota(String arquivo, Metrica metrica, String gabarito) {
        return TABELA.stream()
                .filter(reducao -> reducao.arquivo().equals(arquivo) && reducao.metrica() == metrica)
                .findFirst()
                .map(reducao -> aplicar(reducao, gabarito))
                .orElse(gabarito);
    }

    private static String aplicar(Reducao reducao, String gabarito) {
        if (!reducao.gabarito().equals(gabarito)) {
            throw new IllegalStateException(
                    "%s (%s): o gabarito '%s' difere do registrado no Adendo 6 ('%s'); a reducao nao se aplica."
                            .formatted(reducao.arquivo(), reducao.metrica(), gabarito, reducao.gabarito()));
        }
        return reducao.nota();
    }
}
