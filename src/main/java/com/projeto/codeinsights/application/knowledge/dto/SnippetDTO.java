package com.projeto.codeinsights.application.knowledge.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SnippetDTO(
        UUID id,
        UUID autorId,
        UUID resolucaoId,
        String titulo,
        String codigo,
        String descricao,
        String categoriaConceito,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao) {
}
