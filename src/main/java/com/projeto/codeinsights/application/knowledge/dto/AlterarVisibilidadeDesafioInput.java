package com.projeto.codeinsights.application.knowledge.dto;

import java.util.UUID;

public record AlterarVisibilidadeDesafioInput(
        UUID desafioId,
        UUID solicitanteId,
        boolean publico) {
}
