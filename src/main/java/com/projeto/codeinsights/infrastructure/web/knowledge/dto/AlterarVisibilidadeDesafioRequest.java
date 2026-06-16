package com.projeto.codeinsights.infrastructure.web.knowledge.dto;

import jakarta.validation.constraints.NotNull;

public record AlterarVisibilidadeDesafioRequest(
        @NotNull Boolean publico) {
}
