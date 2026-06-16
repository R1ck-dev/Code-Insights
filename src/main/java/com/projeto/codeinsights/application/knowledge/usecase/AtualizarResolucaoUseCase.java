package com.projeto.codeinsights.application.knowledge.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.AtualizarResolucaoInput;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtualizarResolucaoUseCase {

    private final ResolucaoRepository resolucaoRepository;

    @Transactional
    public void execute(AtualizarResolucaoInput input) {
        Resolucao resolucao = resolucaoRepository.buscarPorId(input.resolucaoId())
                .orElseThrow(() -> new NegocioException("Resolucao nao encontrada."));

        if (!resolucao.getAutorId().equals(input.solicitanteId())) {
            throw new NegocioException("Apenas o autor pode atualizar esta resolucao.");
        }

        resolucao.atualizarCodigo(input.linguagem(), input.codigoFonte());
        resolucaoRepository.salvar(resolucao);
    }
}
