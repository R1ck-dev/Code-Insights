package com.projeto.codeinsights.application.knowledge.dto;

import java.util.UUID;

public record AlterarVisibilidadeResolucaoInput(
        UUID resolucaoId,
        UUID solicitanteId,
        boolean publico) {
}
