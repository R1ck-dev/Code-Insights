package com.projeto.codeinsights.application.knowledge.dto;

import java.util.UUID;

public record AtualizarDesafioInput(
        UUID desafioId,
        UUID solicitanteId,
        String titulo,
        String enunciado,
        String plataformaOrigem,
        String identificadorExterno,
        String urlExterna) {
}
