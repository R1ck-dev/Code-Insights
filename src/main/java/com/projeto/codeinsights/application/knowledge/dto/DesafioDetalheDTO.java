package com.projeto.codeinsights.application.knowledge.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.shared.enums.Visibilidade;

public record DesafioDetalheDTO(
        UUID id,
        UUID autorId,
        String autorUsername,
        String titulo,
        String enunciado,
        String plataformaOrigem,
        String identificadorExterno,
        String urlExterna,
        Visibilidade visibilidade,
        long qtdResolucoes,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {
}
