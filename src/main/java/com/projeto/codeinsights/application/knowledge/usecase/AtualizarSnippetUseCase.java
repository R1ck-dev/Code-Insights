package com.projeto.codeinsights.application.knowledge.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.AtualizarSnippetInput;
import com.projeto.codeinsights.domain.knowledge.model.Snippet;
import com.projeto.codeinsights.domain.knowledge.port.SnippetRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtualizarSnippetUseCase {

    private final SnippetRepository snippetRepository;

    @Transactional
    public void execute(AtualizarSnippetInput input) {
        Snippet snippet = snippetRepository.buscarPorId(input.snippetId())
                .orElseThrow(() -> new NegocioException("Snippet nao encontrado."));

        if (!snippet.getAutorId().equals(input.solicitanteId())) {
            throw new NegocioException("Voce nao tem acesso a este snippet.");
        }

        snippet.atualizar(
                input.titulo(),
                input.codigo(),
                input.descricao(),
                input.categoriaConceito());

        snippetRepository.salvar(snippet);
    }
}
