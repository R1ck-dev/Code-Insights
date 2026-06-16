package com.projeto.codeinsights.domain.knowledge.port;

import java.util.Optional;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.model.Snippet;
import com.projeto.codeinsights.domain.shared.Pagina;

public interface SnippetRepository {
    Snippet salvar(Snippet snippet);

    Optional<Snippet> buscarPorId(UUID id);

    Pagina<Snippet> listarPorAutor(UUID autorId, int pagina, int tamanho);

    void remover(UUID id);
}
