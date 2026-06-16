package com.projeto.codeinsights.application.identity.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.identity.enums.RoleUsuario;
import com.projeto.codeinsights.domain.identity.enums.StatusUsuario;

public record MeuPerfilDTO(
        UUID id,
        String username,
        String email,
        RoleUsuario role,
        StatusUsuario status,
        boolean perfilPublico,
        LocalDateTime dataCriacao) {
}
