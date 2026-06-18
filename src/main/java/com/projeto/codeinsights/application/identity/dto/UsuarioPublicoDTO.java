package com.projeto.codeinsights.application.identity.dto;

import java.util.UUID;

import com.projeto.codeinsights.domain.shared.enums.Visibilidade;

public record UsuarioPublicoDTO(
        UUID id,
        String username,
        Visibilidade visibilidadePerfil) {
}
