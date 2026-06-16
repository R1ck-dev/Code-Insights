package com.projeto.codeinsights.infrastructure.web.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmeterResolucaoRequest(
        String linguagem,
        @NotBlank String codigoFonte) {
}
