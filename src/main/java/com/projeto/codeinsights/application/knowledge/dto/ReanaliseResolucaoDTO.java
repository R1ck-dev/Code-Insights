package com.projeto.codeinsights.application.knowledge.dto;

import java.util.List;
import java.util.UUID;

/**
 * Resultado da reanalise de UMA resolucao. {@code mudancas} so vem preenchida quando o status e
 * {@link Status#REPROCESSADA} e o motor atual discordou do que estava gravado.
 */
public record ReanaliseResolucaoDTO(
        UUID resolucaoId,
        Status status,
        List<MudancaMetricaDTO> mudancas) {

    public enum Status {
        /** Metricas recalculadas pelo motor atual e regravadas no lugar das antigas. */
        REPROCESSADA,
        /** Linguagem sem analisador (por ora, so Java tem): nada foi tocado. */
        PULADA,
        /** Linguagem suportada, mas o codigo nao parseou ou o motor quebrou: nada foi tocado. */
        FALHOU
    }
}
