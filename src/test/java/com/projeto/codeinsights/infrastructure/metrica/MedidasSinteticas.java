package com.projeto.codeinsights.infrastructure.metrica;

import java.util.ArrayList;
import java.util.List;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.infrastructure.metrica.CasoDeCorpus.Categoria;
import com.projeto.codeinsights.infrastructure.metrica.MedidaExterna.MotivoRecusa;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Medida;

/**
 * Medidas montadas a mao para os testes do relatorio externo: nenhuma passa pelo motor e nenhuma
 * tem codigo. Os numeros sao de mentira e servem so para exercitar as contas e o texto.
 */
final class MedidasSinteticas {

    static final String FORA_DA_ESCALA = "O(n*m)";

    private MedidasSinteticas() {
    }

    // ---------------------------------------------------------------- externo

    static CasoExterno caso(LinguagemProgramacao linguagem, String ordem, String tempoGabarito, String tempoNota,
            String espacoGabarito, String espacoNota, String tipoTempo) {
        return new CasoExterno(ordem + "-leetcode-" + ordem + "." + AnotacaoJson.chave(linguagem), linguagem,
                ordem, "leetcode", ordem, tempoGabarito, tempoNota, espacoGabarito, espacoNota, tipoTempo);
    }

    /** Caso simples: gabarito igual a nota, tempo {@code O(n)}, espaco {@code O(1)}, pior caso. */
    static CasoExterno caso(LinguagemProgramacao linguagem, String ordem) {
        return caso(linguagem, ordem, "O(n)", "O(n)", "O(1)", "O(1)", "pior-caso");
    }

    static MedidaExterna respondida(CasoExterno caso, String tempo, String espaco) {
        return new MedidaExterna(caso, tempo, NivelConfianca.MEDIA, null, espaco, NivelConfianca.MEDIA, null, null);
    }

    static MedidaExterna recusada(CasoExterno caso, MotivoRecusa motivo, String classeDaFalha) {
        return MedidaExterna.recusada(caso, motivo, classeDaFalha);
    }

    /**
     * Uma linguagem inteira: {@code mensuraveis} casos de tempo na escala, dos quais {@code acertos}
     * o motor acerta (os outros ele superestima), mais {@code foraDaEscala} casos com gabarito fora
     * da escala. Os numeros de ordem ficam entre {@code 100} e {@code 199}.
     */
    static List<MedidaExterna> lote(LinguagemProgramacao linguagem, int acertos, int mensuraveis, int foraDaEscala) {
        List<MedidaExterna> medidas = new ArrayList<>();
        for (int i = 0; i < mensuraveis; i++) {
            CasoExterno caso = caso(linguagem, "1%02d".formatted(i));
            medidas.add(respondida(caso, i < acertos ? "O(n)" : "O(n^2)", "O(1)"));
        }
        for (int i = 0; i < foraDaEscala; i++) {
            CasoExterno caso = caso(linguagem, "2%02d".formatted(i), FORA_DA_ESCALA, FORA_DA_ESCALA, "O(1)", "O(1)",
                    "pior-caso");
            medidas.add(respondida(caso, "O(n)", "O(1)"));
        }
        return medidas;
    }

    // ---------------------------------------------------------------- proprio

    /**
     * O corpus proprio de mentira: por linguagem, {@code n} casos de gabarito {@code O(n)} (e espaco
     * {@code O(1)}) dos quais {@code acertos} o motor acerta no tempo.
     */
    static List<Medida> proprio(int acertosJava, int nJava, int acertosC, int nC) {
        List<Medida> medidas = new ArrayList<>();
        medidas.addAll(proprio(LinguagemProgramacao.JAVA, acertosJava, nJava));
        medidas.addAll(proprio(LinguagemProgramacao.C, acertosC, nC));
        return medidas;
    }

    private static List<Medida> proprio(LinguagemProgramacao linguagem, int acertos, int n) {
        List<Medida> medidas = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            CasoDeCorpus caso = new CasoDeCorpus("proprio " + i, linguagem, Categoria.CANONICO, "p" + i, "",
                    "sintetico", null, "O(n)", "O(n)", "O(1)", "O(1)");
            medidas.add(new Medida(caso, i < acertos ? "O(n)" : "O(n^2)", NivelConfianca.ALTA, "O(1)",
                    NivelConfianca.ALTA));
        }
        return medidas;
    }
}
