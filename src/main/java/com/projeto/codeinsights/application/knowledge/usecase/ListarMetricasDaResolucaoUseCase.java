package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.ResultadoMetricaDTO;
import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResultadoMetricaRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListarMetricasDaResolucaoUseCase {

    private final ResolucaoRepository resolucaoRepository;
    private final DesafioRepository desafioRepository;
    private final ResultadoMetricaRepository resultadoMetricaRepository;

    @Transactional(readOnly = true)
    public List<ResultadoMetricaDTO> execute(UUID resolucaoId, UUID solicitanteId) {
        Resolucao resolucao = resolucaoRepository.buscarPorId(resolucaoId)
                .orElseThrow(() -> new NegocioException("Resolucao nao encontrada."));

        Desafio desafio = desafioRepository.buscarPorId(resolucao.getDesafioId())
                .orElseThrow(() -> new NegocioException("Desafio nao encontrado."));

        if (!resolucao.pertenceA(solicitanteId) && !desafio.ehPublico()) {
            throw new NegocioException("Voce nao tem permissao para ver estas metricas.");
        }

        return resultadoMetricaRepository.listarPorResolucao(resolucaoId).stream()
                .map(resultado -> new ResultadoMetricaDTO(
                        resultado.getTipo(),
                        resultado.getValor(),
                        resultado.getRotulo(),
                        resultado.getDetalhe(),
                        resultado.getAnalisadoEm()))
                .toList();
    }
}
