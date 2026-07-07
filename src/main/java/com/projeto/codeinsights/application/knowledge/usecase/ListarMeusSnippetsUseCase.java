package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.SnippetDTO;
import com.projeto.codeinsights.domain.knowledge.enums.CategoriaConceito;
import com.projeto.codeinsights.domain.knowledge.model.Snippet;
import com.projeto.codeinsights.domain.knowledge.port.SnippetRepository;
import com.projeto.codeinsights.domain.shared.Pagina;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListarMeusSnippetsUseCase {

    private final SnippetRepository snippetRepository;

    @Transactional(readOnly = true)
    public Pagina<SnippetDTO> execute(UUID autorId, UUID desafioId, CategoriaConceito categoria,
            int pagina, int tamanho) {
        // Prioridade do filtro: desafio (para a tela do desafio) > categoria > todos.
        Pagina<Snippet> pagina_;
        if (desafioId != null) {
            pagina_ = snippetRepository.listarPorAutorEDesafio(autorId, desafioId, pagina, tamanho);
        } else if (categoria != null) {
            pagina_ = snippetRepository.listarPorAutorECategoria(autorId, categoria, pagina, tamanho);
        } else {
            pagina_ = snippetRepository.listarPorAutor(autorId, pagina, tamanho);
        }

        return new Pagina<>(
                pagina_.itens().stream()
                        .map(snippet -> new SnippetDTO(
                                snippet.getId(),
                                snippet.getAutorId(),
                                snippet.getDesafioId(),
                                snippet.getCodigo(),
                                snippet.getDescricao(),
                                snippet.getCategoria(),
                                snippet.getCriadoEm()))
                        .toList(),
                pagina_.paginaAtual(),
                pagina_.totalPaginas(),
                pagina_.totalItens());
    }
}
