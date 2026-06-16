package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemoverResolucaoUseCase {

    private final ResolucaoRepository resolucaoRepository;

    @Transactional
    public void execute(UUID resolucaoId, UUID solicitanteId) {
        Resolucao resolucao = resolucaoRepository.buscarPorId(resolucaoId)
                .orElseThrow(() -> new NegocioException("Resolucao nao encontrada."));

        if (!resolucao.getAutorId().equals(solicitanteId)) {
            throw new NegocioException("Apenas o autor pode remover esta resolucao.");
        }

        resolucaoRepository.remover(resolucaoId);
    }
}
