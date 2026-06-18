package com.projeto.codeinsights.infrastructure.web.knowledge.dto;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmeterResolucaoRequest(
        @NotBlank String codigoFonte,
        @NotNull LinguagemProgramacao linguagem,
        @NotNull @Min(1) @Max(5) Integer indiceAutonomiaIA,
        String descricaoApoioIA) {
}
