package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.ResumoDashboardDTO.EvolucaoMensalDTO;
import com.projeto.codeinsights.domain.knowledge.enums.GranularidadeTempo;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Serie de evolucao do autor (autonomia + complexidade tipica por periodo) na
 * granularidade pedida (dia/semana/mes). Alimenta o grafico do dashboard, que
 * troca a escala temporal sem recarregar os demais agregados do resumo.
 */
@Service
@RequiredArgsConstructor
public class ObterEvolucaoUseCase {

    private final ResolucaoRepository resolucaoRepository;

    @Transactional(readOnly = true)
    public List<EvolucaoMensalDTO> execute(UUID autorId, GranularidadeTempo granularidade) {
        return resolucaoRepository.evolucaoPorAutor(autorId, granularidade).stream()
                .map(p -> new EvolucaoMensalDTO(p.ano(), p.mes(), p.dia(), p.mediaAutonomia(),
                        p.totalResolucoes(), p.mediaComplexidade()))
                .toList();
    }
}
