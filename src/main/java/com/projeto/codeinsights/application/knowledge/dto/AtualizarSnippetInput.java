package com.projeto.codeinsights.application.knowledge.dto;

import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.CategoriaConceito;

public record AtualizarSnippetInput(
        UUID snippetId,
        UUID solicitanteId,
        String codigo,
        String descricao,
        CategoriaConceito categoria) {
}
