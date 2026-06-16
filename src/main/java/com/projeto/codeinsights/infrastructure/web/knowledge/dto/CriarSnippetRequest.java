package com.projeto.codeinsights.infrastructure.web.knowledge.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarSnippetRequest(
        UUID resolucaoId,
        @NotBlank @Size(max = 255) String titulo,
        @NotBlank String codigo,
        String descricao,
        @Size(max = 100) String categoriaConceito) {
}
