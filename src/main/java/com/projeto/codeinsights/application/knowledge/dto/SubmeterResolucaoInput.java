package com.projeto.codeinsights.application.knowledge.dto;

import java.util.UUID;

public record SubmeterResolucaoInput(
        UUID desafioId,
        UUID solicitanteId,
        String linguagem,
        String codigoFonte) {
}
