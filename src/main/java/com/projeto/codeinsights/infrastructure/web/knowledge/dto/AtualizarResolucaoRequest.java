package com.projeto.codeinsights.infrastructure.web.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record AtualizarResolucaoRequest(
        String linguagem,
        @NotBlank String codigoFonte) {
}
