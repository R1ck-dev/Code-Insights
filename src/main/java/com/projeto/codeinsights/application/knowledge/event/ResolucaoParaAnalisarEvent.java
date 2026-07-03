package com.projeto.codeinsights.application.knowledge.event;

import java.util.UUID;

/**
 * Evento publicado quando uma resolucao e submetida ou tem o codigo atualizado,
 * sinalizando que suas metricas estaticas devem ser (re)calculadas. Consumido
 * de forma assincrona, apos o commit da transacao de escrita.
 */
public record ResolucaoParaAnalisarEvent(UUID resolucaoId) {
}
