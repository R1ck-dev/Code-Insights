package com.projeto.codeinsights.infrastructure.metrica;

import java.util.ArrayList;
import java.util.List;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Medida;

/**
 * A base congelada do veredito de H3 (Adendo 6, item 6, e Adendo 7, item 3): o tempo do corpus
 * proprio na remedicao da IC-13, 54/55 em Java e 37/40 em C. O veredito usa estas constantes, nao o
 * que a execucao mediu; a execucao so confere que o corpus proprio ainda da estes numeros, e para
 * antes de tocar o externo se der outros.
 */
final class BaseDoProprio {

    static final TaxaDeAcerto TEMPO_JAVA = new TaxaDeAcerto(54, 55);
    static final TaxaDeAcerto TEMPO_C = new TaxaDeAcerto(37, 40);

    private BaseDoProprio() {
    }

    static TaxaDeAcerto tempo(LinguagemProgramacao linguagem) {
        return switch (linguagem) {
            case JAVA -> TEMPO_JAVA;
            case C -> TEMPO_C;
            default -> throw new IllegalArgumentException("Sem base do corpus proprio para " + linguagem);
        };
    }

    /** Falha, sem citar nada alem de numeros, se o tempo medido do proprio nao for a base. */
    static void exigir(List<Medida> proprias) {
        List<String> divergencias = new ArrayList<>();
        for (LinguagemProgramacao linguagem : GabaritoExterno.LINGUAGENS) {
            List<Medida> daLinguagem = proprias.stream().filter(m -> m.caso().linguagem() == linguagem).toList();
            TaxaDeAcerto medida = Desempenho.doProprioTempo(daLinguagem).principal();
            TaxaDeAcerto base = tempo(linguagem);
            if (!medida.equals(base)) {
                divergencias.add("%s: medido %d/%d, base %d/%d".formatted(linguagem, medida.acertos(),
                        medida.denominador(), base.acertos(), base.denominador()));
            }
        }
        if (!divergencias.isEmpty()) {
            throw new IllegalStateException("O corpus proprio nao deu a base congelada do Adendo 6 ("
                    + String.join("; ", divergencias) + "). O corpus externo nao foi tocado.");
        }
    }
}
