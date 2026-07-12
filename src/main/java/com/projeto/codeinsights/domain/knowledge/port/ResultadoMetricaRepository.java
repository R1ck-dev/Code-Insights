package com.projeto.codeinsights.domain.knowledge.port;

import java.util.List;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.ContagemMetrica;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;

public interface ResultadoMetricaRepository {
    void salvarTodos(List<ResultadoMetrica> resultados);

    List<ResultadoMetrica> listarPorResolucao(UUID resolucaoId);

    /**
     * Metricas de um unico {@code tipo} para um lote de resolucoes, numa unica consulta.
     * <p>
     * Existe para as LISTAS: montar o resumo de uma pagina de resolucoes chamando
     * {@link #listarPorResolucao(UUID)} dentro do laco seria um N+1. Resolucoes sem o
     * resultado pedido (nao analisadas, ou linguagem sem analisador) simplesmente nao
     * aparecem no retorno — ausencia, e nao zero.
     */
    List<ResultadoMetrica> listarPorResolucoesETipo(List<UUID> resolucaoIds, TipoMetrica tipo);

    /** Distribuicao (contagem por rotulo/ordinal) de um tipo de metrica entre as resolucoes do autor. */
    List<ContagemMetrica> contarPorRotulo(UUID autorId, TipoMetrica tipo);

    void removerPorResolucao(UUID resolucaoId);
}
