package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemoverDesafioUseCase {

    private final DesafioRepository desafioRepository;

    @Transactional
    public void execute(UUID desafioId, UUID solicitanteId) {
        Desafio desafio = desafioRepository.buscarPorId(desafioId)
                .orElseThrow(() -> new NegocioException("Desafio nao encontrado."));

        if (!desafio.pertenceA(solicitanteId)) {
            throw new NegocioException("Voce nao tem permissao para remover este desafio.");
        }

        desafioRepository.remover(desafioId);
    }
}
