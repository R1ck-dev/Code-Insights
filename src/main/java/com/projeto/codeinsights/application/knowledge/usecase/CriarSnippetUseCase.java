package com.projeto.codeinsights.application.knowledge.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.CriarSnippetInput;
import com.projeto.codeinsights.application.knowledge.dto.SnippetDTO;
import com.projeto.codeinsights.domain.knowledge.model.Snippet;
import com.projeto.codeinsights.domain.knowledge.port.SnippetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CriarSnippetUseCase {

    private final SnippetRepository snippetRepository;

    @Transactional
    public SnippetDTO execute(CriarSnippetInput input) {
        Snippet snippet = new Snippet(
                null,
                input.autorId(),
                input.codigo(),
                input.descricao(),
                input.categoria());

        Snippet salvo = snippetRepository.salvar(snippet);

        return new SnippetDTO(
                salvo.getId(),
                salvo.getAutorId(),
                salvo.getCodigo(),
                salvo.getDescricao(),
                salvo.getCategoria(),
                salvo.getCriadoEm());
    }
}
