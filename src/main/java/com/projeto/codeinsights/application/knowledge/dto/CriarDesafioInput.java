package com.projeto.codeinsights.application.knowledge.dto;

import java.util.UUID;

public record CriarDesafioInput(
        UUID autorId,
        String titulo,
        String enunciado,
        String plataformaOrigem,
        String identificadorExterno,
        String urlExterna) {
}
