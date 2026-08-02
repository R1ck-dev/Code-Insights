package com.projeto.codeinsights.application.identity.dto;

import java.util.UUID;

import com.projeto.codeinsights.domain.identity.enums.Role;

/** Retorno da mudanca de papel de um usuario pela administracao. */
public record PapelAlteradoDTO(UUID id, String username, String email, Role role) {
}
