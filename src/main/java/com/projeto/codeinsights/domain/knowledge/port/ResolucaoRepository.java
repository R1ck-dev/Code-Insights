package com.projeto.codeinsights.domain.knowledge.port;

import java.util.Optional;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.shared.Pagina;

public interface ResolucaoRepository {
    Resolucao salvar(Resolucao resolucao);

    Optional<Resolucao> buscarPorId(UUID id);

    Pagina<Resolucao> listarPorDesafio(UUID desafioId, int pagina, int tamanho);

    long contarPorDesafio(UUID desafioId);

    void remover(UUID id);
}
