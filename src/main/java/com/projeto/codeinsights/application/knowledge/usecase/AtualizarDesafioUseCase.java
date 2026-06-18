package com.projeto.codeinsights.application.knowledge.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.AtualizarDesafioInput;
import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtualizarDesafioUseCase {

    private final DesafioRepository desafioRepository;

    @Transactional
    public void execute(AtualizarDesafioInput input) {
        Desafio desafio = desafioRepository.buscarPorId(input.desafioId())
                .orElseThrow(() -> new NegocioException("Desafio nao encontrado."));

        if (!desafio.pertenceA(input.solicitanteId())) {
            throw new NegocioException("Voce nao tem permissao para alterar este desafio.");
        }

        desafio.atualizarDetalhes(
                input.titulo() != null ? input.titulo() : desafio.getTitulo(),
                input.enunciado() != null ? input.enunciado() : desafio.getEnunciado(),
                input.plataformaOrigem() != null ? input.plataformaOrigem() : desafio.getPlataformaOrigem(),
                input.identificadorExterno() != null ? input.identificadorExterno() : desafio.getIdentificadorExterno(),
                input.urlExterna() != null ? input.urlExterna() : desafio.getUrlExterna());

        desafioRepository.salvar(desafio);
    }
}
