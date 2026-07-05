package com.projeto.codeinsights.domain.knowledge.port;

import java.util.Optional;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.CategoriaConceito;
import com.projeto.codeinsights.domain.knowledge.model.Snippet;
import com.projeto.codeinsights.domain.shared.Pagina;

public interface SnippetRepository {
    Snippet salvar(Snippet snippet);

    Optional<Snippet> buscarPorId(UUID id);

    Pagina<Snippet> listarPorAutor(UUID autorId, int pagina, int tamanho);

    Pagina<Snippet> listarPorAutorECategoria(UUID autorId, CategoriaConceito categoria, int pagina, int tamanho);

    long contarPorAutor(UUID autorId);

    /** Quantas categorias de conceito distintas o autor usa nos seus snippets. */
    long contarCategoriasPorAutor(UUID autorId);

    void remover(UUID id);
}
