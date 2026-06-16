package com.projeto.codeinsights.application.knowledge.dto;

import java.util.UUID;

public record AtualizarSnippetInput(
        UUID snippetId,
        UUID solicitanteId,
        String titulo,
        String codigo,
        String descricao,
        String categoriaConceito) {
}
