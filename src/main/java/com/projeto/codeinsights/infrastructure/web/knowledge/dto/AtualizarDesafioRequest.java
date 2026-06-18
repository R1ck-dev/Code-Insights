package com.projeto.codeinsights.infrastructure.web.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarDesafioRequest(
        @NotBlank @Size(max = 255) String titulo,
        String enunciado,
        @Size(max = 100) String plataformaOrigem,
        @Size(max = 100) String identificadorExterno,
        @Size(max = 500) String urlExterna) {
}
