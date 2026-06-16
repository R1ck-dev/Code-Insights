package com.projeto.codeinsights.infrastructure.web.knowledge.dto;

import com.projeto.codeinsights.domain.knowledge.enums.DificuldadeDesafio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarDesafioRequest(
        @NotBlank @Size(max = 255) String titulo,
        String descricao,
        @Size(max = 100) String origemPlataforma,
        DificuldadeDesafio dificuldade) {
}
