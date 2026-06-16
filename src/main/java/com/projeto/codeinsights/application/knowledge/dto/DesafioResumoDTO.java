package com.projeto.codeinsights.application.knowledge.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.DificuldadeDesafio;

public record DesafioResumoDTO(
        UUID id,
        UUID autorId,
        String autorUsername,
        String titulo,
        String origemPlataforma,
        DificuldadeDesafio dificuldade,
        boolean publico,
        LocalDateTime dataCriacao) {
}
