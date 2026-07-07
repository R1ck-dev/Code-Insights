package com.projeto.codeinsights.application.knowledge.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.CriarSnippetInput;
import com.projeto.codeinsights.application.knowledge.dto.SnippetDTO;
import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.model.Snippet;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.knowledge.port.SnippetRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CriarSnippetUseCase {

    private final SnippetRepository snippetRepository;
    private final DesafioRepository desafioRepository;

    @Transactional
    public SnippetDTO execute(CriarSnippetInput input) {
        // Vinculo a desafio e opcional; quando presente, so pode ser um desafio do proprio autor.
        if (input.desafioId() != null) {
            Desafio desafio = desafioRepository.buscarPorId(input.desafioId())
                    .orElseThrow(() -> new NegocioException("Desafio nao encontrado."));
            if (!desafio.pertenceA(input.autorId())) {
                throw new NegocioException("Voce so pode vincular snippets aos seus proprios desafios.");
            }
        }

        Snippet snippet = new Snippet(
                null,
                input.autorId(),
                input.desafioId(),
                input.codigo(),
                input.descricao(),
                input.categoria());

        Snippet salvo = snippetRepository.salvar(snippet);

        return new SnippetDTO(
                salvo.getId(),
                salvo.getAutorId(),
                salvo.getDesafioId(),
                salvo.getCodigo(),
                salvo.getDescricao(),
                salvo.getCategoria(),
                salvo.getCriadoEm());
    }
}
