package com.projeto.codeinsights.infrastructure.web.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record ExcluirMinhaContaRequest(@NotBlank String senhaAtual) {
}
