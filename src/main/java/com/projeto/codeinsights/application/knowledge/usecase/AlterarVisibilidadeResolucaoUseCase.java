package com.projeto.codeinsights.application.knowledge.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.AlterarVisibilidadeResolucaoInput;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlterarVisibilidadeResolucaoUseCase {

    private final ResolucaoRepository resolucaoRepository;

    @Transactional
    public void execute(AlterarVisibilidadeResolucaoInput input) {
        Resolucao resolucao = resolucaoRepository.buscarPorId(input.resolucaoId())
                .orElseThrow(() -> new NegocioException("Resolucao nao encontrada."));

        if (!resolucao.pertenceA(input.solicitanteId())) {
            throw new NegocioException("Voce nao tem permissao para alterar esta resolucao.");
        }

        if (input.publico()) {
            resolucao.publicar();
        } else {
            resolucao.ocultar();
        }

        resolucaoRepository.salvar(resolucao);
    }
}
