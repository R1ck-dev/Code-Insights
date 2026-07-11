package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.PontoCartaDTO;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Carta celeste do autor: TODAS as suas resolucoes como pontos, sem o limite da
 * "atividade recente" do dashboard — a carta so faz sentido com a amostra inteira.
 */
@Service
@RequiredArgsConstructor
public class ObterCartaCelesteUseCase {

    private final ResolucaoRepository resolucaoRepository;

    @Transactional(readOnly = true)
    public List<PontoCartaDTO> execute(UUID autorId) {
        return resolucaoRepository.listarPontosCartaPorAutor(autorId).stream()
                .map(p -> new PontoCartaDTO(p.resolucaoId(), p.desafioId(), p.desafioTitulo(),
                        p.linguagem(), p.indiceAutonomiaIA(), p.analisada(),
                        p.tempoRotulo(), p.tempoOrdem(), p.confiancaTempo(),
                        p.espacoRotulo(), p.espacoOrdem(), p.ciclomatica(),
                        p.visibilidade(), p.submetidaEm()))
                .toList();
    }
}
