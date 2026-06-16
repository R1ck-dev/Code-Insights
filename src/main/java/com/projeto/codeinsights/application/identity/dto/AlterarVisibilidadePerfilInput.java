package com.projeto.codeinsights.application.identity.dto;

import java.util.UUID;

public record AlterarVisibilidadePerfilInput(
        UUID usuarioId,
        boolean publico) {
}
