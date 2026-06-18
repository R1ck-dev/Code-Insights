package com.projeto.codeinsights.infrastructure.web.knowledge.dto;

import com.projeto.codeinsights.domain.knowledge.enums.CategoriaConceito;

public record AtualizarSnippetRequest(
        String codigo,
        String descricao,
        CategoriaConceito categoria) {
}
