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

        if (!resolucao.pertenceA(input.solicitanteId())) {
            throw new NegocioException("Apenas o autor pode atualizar esta resolucao.");
        }

        resolucao.atualizarCodigo(input.codigoFonte(), input.linguagem());
        resolucaoRepository.salvar(resolucao);
    }
}
