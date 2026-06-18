package com.projeto.codeinsights.application.knowledge.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.CategoriaConceito;

public record SnippetDTO(
        UUID id,
        UUID autorId,
        String codigo,
        String descricao,
        CategoriaConceito categoria,
        OffsetDateTime criadoEm) {
}
