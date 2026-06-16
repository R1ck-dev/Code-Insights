package com.projeto.codeinsights.application.knowledge.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.AlterarVisibilidadeDesafioInput;
import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlterarVisibilidadeDesafioUseCase {

    private final DesafioRepository desafioRepository;

    @Transactional
    public void execute(AlterarVisibilidadeDesafioInput input) {
        Desafio desafio = desafioRepository.buscarPorId(input.desafioId())
                .orElseThrow(() -> new NegocioException("Desafio nao encontrado."));

        if (!desafio.getAutorId().equals(input.solicitanteId())) {
            throw new NegocioException("Voce nao tem permissao para alterar este desafio.");
        }

        if (input.publico()) {
            desafio.publicar();
        } else {
            desafio.despublicar();
        }

        desafioRepository.salvar(desafio);
    }
}
