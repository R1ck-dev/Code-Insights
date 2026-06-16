package com.projeto.codeinsights.application.knowledge.dto;

import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.DificuldadeDesafio;

public record CriarDesafioInput(
        UUID autorId,
        String titulo,
        String descricao,
        String origemPlataforma,
        DificuldadeDesafio dificuldade) {
}
