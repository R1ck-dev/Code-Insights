package com.projeto.codeinsights.application.identity.dto;

import java.util.UUID;

public record AtualizarMeuPerfilInput(
        UUID usuarioId,
        String username) {
}
