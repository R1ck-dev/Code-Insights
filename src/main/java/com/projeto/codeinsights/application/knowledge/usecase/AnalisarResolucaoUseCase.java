package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.domain.knowledge.port.AnalisadorMetricas;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResultadoMetricaRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

/**
 * Calcula (ou recalcula) as metricas estaticas de uma resolucao e as persiste.
 * Idempotente: remove os resultados anteriores antes de gravar os novos, para
 * suportar reanalise apos atualizacao de codigo. Ao final marca a resolucao como
 * analisada.
 */
@Service
@RequiredArgsConstructor
public class AnalisarResolucaoUseCase {

    private final ResolucaoRepository resolucaoRepository;
    private final ResultadoMetricaRepository resultadoMetricaRepository;
    private final AnalisadorMetricas analisadorMetricas;

    @Transactional
    public void execute(UUID resolucaoId) {
        Resolucao resolucao = resolucaoRepository.buscarPorId(resolucaoId)
                .orElseThrow(() -> new NegocioException("Resolucao nao encontrada."));

        resultadoMetricaRepository.removerPorResolucao(resolucaoId);

        List<ResultadoMetrica> resultados = analisadorMetricas.analisar(resolucao);
        if (!resultados.isEmpty()) {
            resultadoMetricaRepository.salvarTodos(resultados);
        }

        resolucao.marcarComoAnalisada();
        resolucaoRepository.salvar(resolucao);
    }
}
