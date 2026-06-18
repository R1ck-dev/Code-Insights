package com.projeto.codeinsights.application.identity.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.identity.enums.Role;
import com.projeto.codeinsights.domain.identity.enums.StatusConta;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;

public record MeuPerfilDTO(
        UUID id,
        String username,
        String email,
        Role role,
        StatusConta status,
        Visibilidade visibilidadePerfil,
        OffsetDateTime criadoEm) {
}
