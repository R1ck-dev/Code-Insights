package com.projeto.codeinsights.application.knowledge.usecase;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.ResolucaoResumoDTO;
import com.projeto.codeinsights.application.knowledge.dto.SubmeterResolucaoInput;
import com.projeto.codeinsights.application.knowledge.event.ResolucaoParaAnalisarEvent;
import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubmeterResolucaoUseCase {

    private final ResolucaoRepository resolucaoRepository;
    private final DesafioRepository desafioRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ResolucaoResumoDTO execute(SubmeterResolucaoInput input) {
        Desafio desafio = desafioRepository.buscarPorId(input.desafioId())
                .orElseThrow(() -> new NegocioException("Desafio nao encontrado."));

        if (!desafio.pertenceA(input.solicitanteId())) {
            throw new NegocioException("Apenas o autor do desafio pode submeter resolucoes.");
        }

        Resolucao resolucao = new Resolucao(
                null,
                input.solicitanteId(),
                input.desafioId(),
                input.codigoFonte(),
                input.linguagem(),
                input.indiceAutonomiaIA(),
                input.descricaoApoioIA());

        Resolucao salva = resolucaoRepository.salvar(resolucao);
        eventPublisher.publishEvent(new ResolucaoParaAnalisarEvent(salva.getId()));

        // Metricas nulas: a analise e assincrona (evento acima) e ainda nao rodou. Nulo = sem dado,
        // e nao O(1) — a lista mostrara "calculando" ate a analise concluir.
        return new ResolucaoResumoDTO(
                salva.getId(),
                salva.getDesafioId(),
                salva.getAutorId(),
                salva.getLinguagem(),
                salva.getIndiceAutonomiaIA(),
                salva.getVisibilidade(),
                salva.isAnalisada(),
                null,
                null,
                null,
                salva.getSubmetidaEm());
    }
}
