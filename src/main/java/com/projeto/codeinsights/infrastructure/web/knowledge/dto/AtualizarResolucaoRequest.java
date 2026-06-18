package com.projeto.codeinsights.infrastructure.web.knowledge.dto;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AtualizarResolucaoRequest(
        @NotBlank String codigoFonte,
        @NotNull LinguagemProgramacao linguagem) {
}
