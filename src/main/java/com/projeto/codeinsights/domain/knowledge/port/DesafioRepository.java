package com.projeto.codeinsights.domain.knowledge.port;

import java.util.Optional;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.shared.Pagina;

public interface DesafioRepository {
    Desafio salvar(Desafio desafio);

    Optional<Desafio> buscarPorId(UUID id);

    Pagina<Desafio> listarPorAutor(UUID autorId, int pagina, int tamanho);

    Pagina<Desafio> listarPublicosPorAutor(UUID autorId, int pagina, int tamanho);

    void remover(UUID id);
}
