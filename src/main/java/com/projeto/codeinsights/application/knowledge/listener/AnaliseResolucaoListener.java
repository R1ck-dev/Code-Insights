package com.projeto.codeinsights.application.knowledge.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.projeto.codeinsights.application.knowledge.event.ResolucaoParaAnalisarEvent;
import com.projeto.codeinsights.application.knowledge.usecase.AnalisarResolucaoUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dispara a analise de metricas em background, apos o commit da transacao que
 * submeteu/atualizou a resolucao. Roda apos o commit para que a analise (em sua
 * propria transacao) enxergue a resolucao ja persistida; erros sao registrados e
 * contidos aqui, sem afetar a requisicao original.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnaliseResolucaoListener {

    private final AnalisarResolucaoUseCase analisarResolucaoUseCase;

    @Async("analiseExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoReceberResolucaoParaAnalisar(ResolucaoParaAnalisarEvent evento) {
        try {
            analisarResolucaoUseCase.execute(evento.resolucaoId());
        } catch (Exception ex) {
            log.error("Falha ao analisar metricas da resolucao {}", evento.resolucaoId(), ex);
        }
    }
}
