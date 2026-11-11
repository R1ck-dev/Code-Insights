package com.projeto.codeinsights.infrastructure.metrica;

import com.projeto.codeinsights.infrastructure.metrica.ExecucaoDoExterno.Modo;

/**
 * Quando a medicao do corpus externo pode rodar. Uma funcao pura: recebe se a pasta local existe e o
 * valor da propriedade {@value #PROPRIEDADE}, e devolve o que fazer e por que. Nao toca em disco nem
 * executa nada, o que deixa a decisao testavel sem o corpus.
 * <p>
 * A medicao e unica (Adendo 6, item 9). Por isso so roda com a pasta presente <b>e</b> um pedido
 * explicito no comando (Adendo 7, item 2); um build comum fica ignorado, com a mensagem de por que.
 */
final class PortaoDaMedicao {

    static final String PROPRIEDADE = "corpus.externo";
    static final String PEDIDO_MEDIR = "medir";
    static final String PEDIDO_REMEDIR = "remedir";

    /** O que o teste faz: nao roda, mede pela primeira vez, ou refaz sobre um bloco ja medido. */
    enum Acao { IGNORAR, MEDIR, REMEDIR }

    record Decisao(Acao acao, String mensagem) {

        boolean executa() {
            return acao != Acao.IGNORAR;
        }

        /** O modo do pipeline; so existe quando a decisao manda executar. */
        Modo modo() {
            return switch (acao) {
                case MEDIR -> Modo.MEDIR;
                case REMEDIR -> Modo.REMEDIR;
                case IGNORAR -> throw new IllegalStateException("Decisao de ignorar nao tem modo de execucao.");
            };
        }
    }

    private PortaoDaMedicao() {
    }

    /**
     * @param pasta        como a pasta local deve aparecer na mensagem
     * @param pastaExiste  se a pasta dos fontes existe
     * @param pedido       valor de {@value #PROPRIEDADE}, ou {@code null} se nao foi passada
     */
    static Decisao decidir(String pasta, boolean pastaExiste, String pedido) {
        if (!pastaExiste) {
            return ignorar("pasta local " + pasta + " ausente: os fontes de terceiros nao entram no git, e o teste "
                    + "le deles (criterio do corpus externo, secao 7). Nada foi medido.");
        }
        if (pedido == null || pedido.isBlank()) {
            return ignorar("medicao unica do corpus externo (Adendo 6, item 9): so roda com pedido explicito, "
                    + "-D" + PROPRIEDADE + "=" + PEDIDO_MEDIR + ". Um build comum nao mede nem sobrescreve a "
                    + "primeira medicao (Adendo 7, item 2).");
        }
        return switch (pedido) {
            case PEDIDO_MEDIR -> new Decisao(Acao.MEDIR, "medicao pedida");
            case PEDIDO_REMEDIR -> new Decisao(Acao.REMEDIR, "remedicao pedida: sobrescreve o bloco externo");
            default -> ignorar("valor '" + pedido + "' de " + PROPRIEDADE + " nao reconhecido; os validos sao "
                    + PEDIDO_MEDIR + " e " + PEDIDO_REMEDIR + " (Adendo 7, item 2). Nada foi medido.");
        };
    }

    private static Decisao ignorar(String mensagem) {
        return new Decisao(Acao.IGNORAR, mensagem);
    }
}
