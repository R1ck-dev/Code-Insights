package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.domain.knowledge.model.Snippet;
import com.projeto.codeinsights.domain.knowledge.port.SnippetRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemoverSnippetUseCase {

    private final SnippetRepository snippetRepository;

    @Transactional
    public void execute(UUID snippetId, UUID solicitanteId) {
        Snippet snippet = snippetRepository.buscarPorId(snippetId)
                .orElseThrow(() -> new NegocioException("Snippet nao encontrado."));

        if (!snippet.getAutorId().equals(solicitanteId)) {
            throw new NegocioException("Voce nao tem acesso a este snippet.");
        }

        snippetRepository.remover(snippetId);
    }
}
