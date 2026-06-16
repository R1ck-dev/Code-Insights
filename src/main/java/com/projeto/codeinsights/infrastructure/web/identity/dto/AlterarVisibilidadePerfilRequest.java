package com.projeto.codeinsights.infrastructure.web.identity.dto;

import jakarta.validation.constraints.NotNull;

public record AlterarVisibilidadePerfilRequest(
        @NotNull(message = "Informe se o perfil deve ser publico.")
        Boolean publico) {
}
