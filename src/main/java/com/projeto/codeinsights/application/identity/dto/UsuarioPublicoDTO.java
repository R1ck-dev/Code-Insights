package com.projeto.codeinsights.application.identity.dto;

import java.util.UUID;

public record UsuarioPublicoDTO(
        UUID id,
        String username,
        boolean perfilPublico) {
}
