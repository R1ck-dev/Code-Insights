package com.projeto.codeinsights.infrastructure.web.knowledge.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.codeinsights.application.knowledge.dto.ResultadoMetricaDTO;
import com.projeto.codeinsights.application.knowledge.dto.ResumoDashboardDTO;
import com.projeto.codeinsights.application.knowledge.dto.ResumoDashboardDTO.EvolucaoMensalDTO;
import com.projeto.codeinsights.application.knowledge.usecase.ListarMetricasDaResolucaoUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.ObterEvolucaoUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.ObterResumoDashboardUseCase;
import com.projeto.codeinsights.domain.knowledge.enums.GranularidadeTempo;
import com.projeto.codeinsights.infrastructure.config.security.CurrentUserId;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MetricaController {

    private final ListarMetricasDaResolucaoUseCase listarMetricasDaResolucaoUseCase;
    private final ObterResumoDashboardUseCase obterResumoDashboardUseCase;
    private final ObterEvolucaoUseCase obterEvolucaoUseCase;

    @GetMapping("/api/resolucoes/{resolucaoId}/metricas")
    public ResponseEntity<List<ResultadoMetricaDTO>> listar(
            @PathVariable UUID resolucaoId,
            @CurrentUserId UUID usuarioId) {
        return ResponseEntity.ok(listarMetricasDaResolucaoUseCase.execute(resolucaoId, usuarioId));
    }

    @GetMapping("/api/metricas/resumo")
    public ResponseEntity<ResumoDashboardDTO> resumo(@CurrentUserId UUID autorId) {
        return ResponseEntity.ok(obterResumoDashboardUseCase.execute(autorId));
    }

    @GetMapping("/api/metricas/evolucao")
    public ResponseEntity<List<EvolucaoMensalDTO>> evolucao(
            @CurrentUserId UUID autorId,
            @RequestParam(defaultValue = "MENSAL") GranularidadeTempo granularidade) {
        return ResponseEntity.ok(obterEvolucaoUseCase.execute(autorId, granularidade));
    }
}
