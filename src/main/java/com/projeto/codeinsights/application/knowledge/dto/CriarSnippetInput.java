package com.projeto.codeinsights.application.knowledge.dto;

import java.util.UUID;

public record CriarSnippetInput(
        UUID autorId,
        UUID resolucaoId,
        String titulo,
        String codigo,
        String descricao,
        String categoriaConceito) {
}
