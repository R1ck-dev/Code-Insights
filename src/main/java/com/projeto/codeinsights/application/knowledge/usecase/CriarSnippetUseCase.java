package com.projeto.codeinsights.application.knowledge.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.CriarSnippetInput;
import com.projeto.codeinsights.application.knowledge.dto.SnippetDTO;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.Snippet;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.knowledge.port.SnippetRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CriarSnippetUseCase {

    private final SnippetRepository snippetRepository;
    private final ResolucaoRepository resolucaoRepository;

    @Transactional
    public SnippetDTO execute(CriarSnippetInput input) {
        if (input.resolucaoId() != null) {
            Resolucao resolucao = resolucaoRepository.buscarPorId(input.resolucaoId())
                    .orElseThrow(() -> new NegocioException("Resolucao nao encontrada."));
            if (!resolucao.getAutorId().equals(input.autorId())) {
                throw new NegocioException("A resolucao informada nao pertence a voce.");
            }
        }

        Snippet snippet = new Snippet(
                null,
                input.autorId(),
                input.resolucaoId(),
                input.titulo(),
                input.codigo(),
                input.descricao(),
                input.categoriaConceito());

        Snippet salvo = snippetRepository.salvar(snippet);

        return new SnippetDTO(
                salvo.getId(),
                salvo.getAutorId(),
                salvo.getResolucaoId(),
                salvo.getTitulo(),
                salvo.getCodigo(),
                salvo.getDescricao(),
                salvo.getCategoriaConceito(),
                salvo.getDataCriacao(),
                salvo.getDataAtualizacao());
    }
}
