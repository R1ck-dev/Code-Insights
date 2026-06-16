package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.ResolucaoResumoDTO;
import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.shared.Pagina;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListarResolucoesDoDesafioUseCase {

    private final ResolucaoRepository resolucaoRepository;
    private final DesafioRepository desafioRepository;

    @Transactional(readOnly = true)
    public Pagina<ResolucaoResumoDTO> execute(UUID desafioId, UUID solicitanteId, int pagina, int tamanho) {
        Desafio desafio = desafioRepository.buscarPorId(desafioId)
                .orElseThrow(() -> new NegocioException("Desafio nao encontrado."));

        if (!desafio.getAutorId().equals(solicitanteId) && !desafio.isPublico()) {
            throw new NegocioException("Voce nao tem permissao para ver as resolucoes deste desafio.");
        }

        Pagina<Resolucao> resolucoes = resolucaoRepository.listarPorDesafio(desafioId, pagina, tamanho);

        return new Pagina<>(
                resolucoes.itens().stream().map(this::toResumo).toList(),
                resolucoes.paginaAtual(),
                resolucoes.totalPaginas(),
                resolucoes.totalItens());
    }

    private ResolucaoResumoDTO toResumo(Resolucao resolucao) {
        return new ResolucaoResumoDTO(
                resolucao.getId(),
                resolucao.getDesafioId(),
                resolucao.getAutorId(),
                resolucao.getLinguagem(),
                resolucao.getDataCriacao());
    }
}
