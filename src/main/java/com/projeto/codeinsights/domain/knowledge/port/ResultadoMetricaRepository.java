package com.projeto.codeinsights.domain.knowledge.port;

import java.util.List;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.ContagemMetrica;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;

public interface ResultadoMetricaRepository {
    void salvarTodos(List<ResultadoMetrica> resultados);

    List<ResultadoMetrica> listarPorResolucao(UUID resolucaoId);

    /** Distribuicao (contagem por rotulo/ordinal) de um tipo de metrica entre as resolucoes do autor. */
    List<ContagemMetrica> contarPorRotulo(UUID autorId, TipoMetrica tipo);

    void removerPorResolucao(UUID resolucaoId);
}
