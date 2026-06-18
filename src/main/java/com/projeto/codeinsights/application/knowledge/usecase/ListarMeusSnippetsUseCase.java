package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.SnippetDTO;
import com.projeto.codeinsights.domain.knowledge.model.Snippet;
import com.projeto.codeinsights.domain.knowledge.port.SnippetRepository;
import com.projeto.codeinsights.domain.shared.Pagina;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListarMeusSnippetsUseCase {

    private final SnippetRepository snippetRepository;

    @Transactional(readOnly = true)
    public Pagina<SnippetDTO> execute(UUID autorId, int pagina, int tamanho) {
        Pagina<Snippet> pagina_ = snippetRepository.listarPorAutor(autorId, pagina, tamanho);

        return new Pagina<>(
                pagina_.itens().stream()
                        .map(snippet -> new SnippetDTO(
                                snippet.getId(),
                                snippet.getAutorId(),
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
