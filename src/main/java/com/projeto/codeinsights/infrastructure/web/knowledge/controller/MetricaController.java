package com.projeto.codeinsights.infrastructure.web.knowledge.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.codeinsights.application.knowledge.dto.ResultadoMetricaDTO;
import com.projeto.codeinsights.application.knowledge.usecase.ListarMetricasDaResolucaoUseCase;
import com.projeto.codeinsights.infrastructure.config.security.CurrentUserId;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MetricaController {

    private final ListarMetricasDaResolucaoUseCase listarMetricasDaResolucaoUseCase;

    @GetMapping("/api/resolucoes/{resolucaoId}/metricas")
    public ResponseEntity<List<ResultadoMetricaDTO>> listar(
            @PathVariable UUID resolucaoId,
            @CurrentUserId UUID usuarioId) {
        return ResponseEntity.ok(listarMetricasDaResolucaoUseCase.execute(resolucaoId, usuarioId));
    }
}
