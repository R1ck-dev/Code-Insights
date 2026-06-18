package com.projeto.codeinsights.infrastructure.web.knowledge.dto;

import com.projeto.codeinsights.domain.knowledge.enums.CategoriaConceito;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AtualizarSnippetRequest(
        @NotBlank String codigo,
        String descricao,
        @NotNull CategoriaConceito categoria) {
}
