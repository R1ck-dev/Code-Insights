package com.projeto.codeinsights.application.knowledge.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

public record ResolucaoResumoDTO(
        UUID id,
        UUID desafioId,
        UUID autorId,
        LinguagemProgramacao linguagem,
        int indiceAutonomiaIA,
        boolean analisada,
        OffsetDateTime submetidaEm) {
}
