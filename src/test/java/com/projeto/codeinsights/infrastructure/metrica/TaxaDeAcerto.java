package com.projeto.codeinsights.infrastructure.metrica;

import java.util.Locale;

import com.projeto.codeinsights.infrastructure.metrica.EstatisticaExata.Intervalo;

/**
 * {@code acertos} em {@code denominador}, com a maneira como o relatorio escreve a taxa e o intervalo
 * de Wilson de 95%. Sem {@code Locale} do sistema: o relatorio e o mesmo em qualquer maquina.
 */
record TaxaDeAcerto(long acertos, long denominador) {

    static final String SEM_CASOS = "sem casos";

    /** {@code 54/55 (98.2%)}, ou {@value #SEM_CASOS} quando o denominador e zero. */
    String texto() {
        return denominador == 0 ? SEM_CASOS : "%d/%d (%s)".formatted(acertos, denominador, percentual());
    }

    String percentual() {
        return denominador == 0 ? "-" : porcento(1.0 * acertos / denominador);
    }

    /** {@code [90.4%; 99.7%]}, ou {@code -} quando o denominador e zero. */
    String intervalo() {
        if (denominador == 0) {
            return "-";
        }
        Intervalo wilson = EstatisticaExata.wilson(acertos, denominador);
        return "[%s; %s]".formatted(porcento(wilson.inferior()), porcento(wilson.superior()));
    }

    private static String porcento(double fracao) {
        return String.format(Locale.ROOT, "%.1f%%", 100.0 * fracao);
    }
}
