package com.projeto.codeinsights.application.knowledge.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResolucaoResumoDTO(
        UUID id,
        UUID desafioId,
        UUID autorId,
        String linguagem,
        LocalDateTime dataCriacao) {
}
