package com.projeto.codeinsights.application.knowledge.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.shared.enums.Visibilidade;

public record DesafioResumoDTO(
        UUID id,
        UUID autorId,
        String autorUsername,
        String titulo,
        String plataformaOrigem,
        Visibilidade visibilidade,
        OffsetDateTime criadoEm) {
}
