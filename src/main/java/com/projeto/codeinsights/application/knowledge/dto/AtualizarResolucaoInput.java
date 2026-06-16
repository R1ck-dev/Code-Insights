package com.projeto.codeinsights.application.knowledge.dto;

import java.util.UUID;

public record AtualizarResolucaoInput(
        UUID resolucaoId,
        UUID solicitanteId,
        String linguagem,
        String codigoFonte) {
}
