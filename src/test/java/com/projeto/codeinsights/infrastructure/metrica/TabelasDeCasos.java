package com.projeto.codeinsights.infrastructure.metrica;

import com.projeto.codeinsights.infrastructure.metrica.MedidaExterna.MotivoRecusa;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Direcao;

/**
 * As tabelas do bloco externo que listam caso a caso: reducoes aplicadas, divergencias, gabaritos
 * fora da escala e casos de tempo esperado. Cada caso aparece pelo identificador da anotacao
 * ({@code 021-3022}) e a linguagem; o relatorio nunca mostra codigo, so a resposta do motor.
 */
final class TabelasDeCasos {

    private static final String MARCA_ESPERADO = "tempo esperado";

    private TabelasDeCasos() {
    }

    /** Os casos em que a nota difere do gabarito (Adendo 6, item 2): as duas formas, lado a lado. */
    static String reducoes(DadosDoBloco dados) {
        TabelaMd tabela = new TabelaMd("Linguagem", "Caso", "Metrica", "Gabarito", "Nota", "Motor", "Direcao");
        int linhas = 0;
        for (ResumoExterno resumo : dados.resumos()) {
            for (MedidaExterna medida : resumo.medidas()) {
                if (medida.caso().tempoReduzido()) {
                    linhaDoTempo(tabela, medida, medida.caso().tempoGabarito(), medida.caso().tempoNota());
                    linhas++;
                }
                if (medida.caso().espacoReduzido()) {
                    linhaDoEspaco(tabela, medida, medida.caso().espacoGabarito(), medida.caso().espacoNota());
                    linhas++;
                }
            }
        }
        return linhas == 0 ? "Nenhuma reducao aplicada neste conjunto.\n\n" : tabela.renderizar();
    }

    /** Casos do denominador principal em que o motor nao deu a classe da nota. */
    static String divergencias(DadosDoBloco dados) {
        TabelaMd tabela = new TabelaMd("Linguagem", "Caso", "Metrica", "Gabarito", "Nota", "Motor", "Direcao");
        int linhas = 0;
        for (ResumoExterno resumo : dados.resumos()) {
            for (MedidaExterna medida : resumo.divergentesNoTempo()) {
                linhaDoTempo(tabela, medida, medida.caso().tempoGabarito(), medida.caso().tempoNota());
                linhas++;
            }
            for (MedidaExterna medida : resumo.divergentesNoEspaco()) {
                linhaDoEspaco(tabela, medida, medida.caso().espacoGabarito(), medida.caso().espacoNota());
                linhas++;
            }
        }
        return linhas == 0 ? "Nenhuma: o motor bate com a nota em todos os casos mensuraveis.\n\n"
                : tabela.renderizar();
    }

    /** Gabaritos que o motor nao consegue expressar: ficam fora do denominador principal. */
    static String foraDaEscala(DadosDoBloco dados) {
        TabelaMd tabela = new TabelaMd("Linguagem", "Caso", "Metrica", "Gabarito", "Resposta do motor (nao conta)");
        int linhas = 0;
        for (ResumoExterno resumo : dados.resumos()) {
            for (MedidaExterna medida : resumo.foraDaEscalaNoTempo()) {
                tabela.linha(resumo.linguagem().toString(), medida.caso().id(), "tempo",
                        medida.caso().tempoGabarito(),
                        resposta(medida.tempo(), medida.recusaTempo(), medida.classeDaFalha()));
                linhas++;
            }
            for (MedidaExterna medida : resumo.foraDaEscalaNoEspaco()) {
                tabela.linha(resumo.linguagem().toString(), medida.caso().id(), "espaco",
                        medida.caso().espacoGabarito(),
                        resposta(medida.espaco(), medida.recusaEspaco(), medida.classeDaFalha()));
                linhas++;
            }
        }
        return linhas == 0 ? "Nenhum: todos os gabaritos cabem nas 8 classes do motor.\n\n" : tabela.renderizar();
    }

    /** Tempo esperado/amortizado: conta como os demais; aqui so se mostra quantos sao e quantos o motor acertou. */
    static String tempoEsperado(DadosDoBloco dados) {
        StringBuilder md = new StringBuilder();
        for (ResumoExterno resumo : dados.resumos()) {
            Desempenho esperado = resumo.tempoEsperado();
            md.append("- %s: %d caso(s) de tempo esperado, %s no denominador principal.\n".formatted(
                    resumo.linguagem(), esperado.anotados(), esperado.principal().texto()));
        }
        return md.append('\n').toString();
    }

    // ---------------------------------------------------------------- linhas

    private static void linhaDoTempo(TabelaMd tabela, MedidaExterna medida, String gabarito, String nota) {
        linha(tabela, medida, "tempo" + marcaDeTempo(medida), gabarito, nota,
                resposta(medida.tempo(), medida.recusaTempo(), medida.classeDaFalha()),
                ResumoExterno.direcaoDoTempo(medida));
    }

    private static void linhaDoEspaco(TabelaMd tabela, MedidaExterna medida, String gabarito, String nota) {
        linha(tabela, medida, "espaco", gabarito, nota,
                resposta(medida.espaco(), medida.recusaEspaco(), medida.classeDaFalha()),
                ResumoExterno.direcaoDoEspaco(medida));
    }

    private static void linha(TabelaMd tabela, MedidaExterna medida, String metrica, String gabarito, String nota,
            String resposta, Direcao direcao) {
        tabela.linha(medida.caso().linguagem().toString(), medida.caso().id(), metrica, gabarito, nota, resposta,
                direcao.rotulo());
    }

    private static String marcaDeTempo(MedidaExterna medida) {
        return medida.caso().tempoEsperado() ? " (" + MARCA_ESPERADO + ")" : "";
    }

    /**
     * A resposta do motor como o relatorio a escreve: o rotulo e, se foi recusa, o motivo em codigo
     * curto (e a classe da excecao, so no caso de falha). Nunca o texto de uma excecao.
     */
    private static String resposta(String rotulo, MotivoRecusa recusa, String classeDaFalha) {
        if (recusa == null) {
            return rotulo;
        }
        return recusa == MotivoRecusa.FALHA && classeDaFalha != null
                ? "%s (%s: %s)".formatted(rotulo, recusa, classeDaFalha)
                : "%s (%s)".formatted(rotulo, recusa);
    }
}
