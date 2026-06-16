package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.ResolucaoDetalheDTO;
import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BuscarResolucaoDetalheUseCase {

    private final ResolucaoRepository resolucaoRepository;
    private final DesafioRepository desafioRepository;

    @Transactional(readOnly = true)
    public ResolucaoDetalheDTO execute(UUID resolucaoId, UUID solicitanteId) {
        Resolucao resolucao = resolucaoRepository.buscarPorId(resolucaoId)
                .orElseThrow(() -> new NegocioException("Resolucao nao encontrada."));

        Desafio desafio = desafioRepository.buscarPorId(resolucao.getDesafioId())
                .orElseThrow(() -> new NegocioException("Desafio nao encontrado."));

        if (!resolucao.getAutorId().equals(solicitanteId) && !desafio.isPublico()) {
            throw new NegocioException("Voce nao tem permissao para ver esta resolucao.");
        }

        return new ResolucaoDetalheDTO(
                resolucao.getId(),
                resolucao.getDesafioId(),
                resolucao.getAutorId(),
                resolucao.getLinguagem(),
                resolucao.getCodigoFonte(),
                resolucao.getIndiceAutonomiaIa(),
                resolucao.getComplexidadeTempo(),
                resolucao.getComplexidadeEspaco(),
                resolucao.getComplexidadeCiclomatica(),
                resolucao.getDataCriacao(),
                resolucao.getDataAtualizacao());
    }
}
