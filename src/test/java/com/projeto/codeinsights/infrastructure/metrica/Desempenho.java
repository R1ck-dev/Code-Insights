package com.projeto.codeinsights.infrastructure.metrica;

import java.util.List;
import java.util.function.Predicate;

import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Direcao;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Medida;

/**
 * Como uma metrica de um conjunto de casos e contada, igual nos dois corpora (Adendo 6, itens 3 e 4, e
 * Adendo 7, item 4):
 * <ul>
 *   <li>{@code anotados}: casos com gabarito anotado para a metrica (espaco {@code null} nao entra);</li>
 *   <li>{@code mensuraveis}: anotados cuja classe cabe na escala do motor, o denominador da taxa
 *       <b>principal</b>;</li>
 *   <li>{@code acertos}: mensuraveis em que o motor deu a classe do gabarito.</li>
 * </ul>
 * A sensibilidade usa os mesmos acertos sobre os {@code anotados}: o que esta fora da escala conta
 * como erro. A cobertura e {@code mensuraveis/anotados}.
 */
record Desempenho(long acertos, long mensuraveis, long anotados) {

    TaxaDeAcerto principal() {
        return new TaxaDeAcerto(acertos, mensuraveis);
    }

    TaxaDeAcerto sensibilidade() {
        return new TaxaDeAcerto(acertos, anotados);
    }

    /** {@code 16/20 (80.0%)}: quanto da amostra a escala consegue expressar. */
    String cobertura() {
        return new TaxaDeAcerto(mensuraveis, anotados).texto();
    }

    static Desempenho doProprioTempo(List<Medida> medidas) {
        return de(medidas, m -> m.caso().tempoGabarito() != null,
                RelatorioDeAcuracia::mensuravelNoTempo, m -> RelatorioDeAcuracia.direcaoDoTempo(m) == Direcao.ACERTO);
    }

    static Desempenho doProprioEspaco(List<Medida> medidas) {
        return de(medidas, m -> m.caso().espacoGabarito() != null,
                RelatorioDeAcuracia::mensuravelNoEspaco, m -> RelatorioDeAcuracia.direcaoDoEspaco(m) == Direcao.ACERTO);
    }

    static Desempenho doExternoTempo(List<MedidaExterna> medidas) {
        return de(medidas, m -> m.caso().tempoNota() != null,
                m -> m.caso().tempoNaEscala(), m -> ResumoExterno.direcaoDoTempo(m) == Direcao.ACERTO);
    }

    static Desempenho doExternoEspaco(List<MedidaExterna> medidas) {
        return de(medidas, ResumoExterno::espacoAnotado,
                m -> m.caso().espacoNaEscala(), m -> ResumoExterno.direcaoDoEspaco(m) == Direcao.ACERTO);
    }

    private static <T> Desempenho de(List<T> casos, Predicate<T> anotado, Predicate<T> mensuravel,
            Predicate<T> acerto) {
        List<T> anotados = casos.stream().filter(anotado).toList();
        List<T> mensuraveis = anotados.stream().filter(mensuravel).toList();
        return new Desempenho(mensuraveis.stream().filter(acerto).count(), mensuraveis.size(), anotados.size());
    }
}
