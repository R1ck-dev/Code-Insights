package com.projeto.codeinsights.application.knowledge.dto;

import java.util.List;
import java.util.UUID;

/**
 * Relatorio de uma passada de reanalise do corpus.
 * <p>
 * {@code total = reprocessadas + puladas + falhas}. {@code comMudanca} conta as reprocessadas em
 * que ao menos uma metrica trocou de rotulo ou de confianca — e o numero que diz se o
 * reprocessamento fez algo; {@code alteracoes} detalha cada uma delas.
 */
public record RelatorioReanaliseDTO(
        int total,
        int reprocessadas,
        int puladas,
        int falhas,
        int comMudanca,
        List<ResolucaoAlteradaDTO> alteracoes) {

    /** Uma resolucao cuja classificacao mudou, com o antes -> depois de cada metrica afetada. */
    public record ResolucaoAlteradaDTO(UUID resolucaoId, List<MudancaMetricaDTO> mudancas) {
    }
}
