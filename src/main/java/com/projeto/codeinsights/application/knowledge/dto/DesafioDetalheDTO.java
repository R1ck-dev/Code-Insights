package com.projeto.codeinsights.application.knowledge.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.DificuldadeDesafio;

public record DesafioDetalheDTO(
        UUID id,
        UUID autorId,
        String autorUsername,
        String titulo,
        String descricao,
        String origemPlataforma,
        DificuldadeDesafio dificuldade,
        boolean publico,
        long qtdResolucoes,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao) {
}
