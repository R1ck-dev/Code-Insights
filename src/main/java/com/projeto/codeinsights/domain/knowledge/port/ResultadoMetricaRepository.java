package com.projeto.codeinsights.domain.knowledge.port;

import java.util.List;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;

public interface ResultadoMetricaRepository {
    void salvarTodos(List<ResultadoMetrica> resultados);

    List<ResultadoMetrica> listarPorResolucao(UUID resolucaoId);

    void removerPorResolucao(UUID resolucaoId);
}
