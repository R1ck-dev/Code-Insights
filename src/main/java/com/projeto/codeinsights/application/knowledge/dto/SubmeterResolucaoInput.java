package com.projeto.codeinsights.application.knowledge.dto;

import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

public record SubmeterResolucaoInput(
        UUID desafioId,
        UUID solicitanteId,
        String codigoFonte,
        LinguagemProgramacao linguagem,
        int indiceAutonomiaIA,
        String descricaoApoioIA) {
}
