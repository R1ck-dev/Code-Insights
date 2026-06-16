package com.projeto.codeinsights.infrastructure.web.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizarMeuPerfilRequest(
        @NotBlank(message = "O nome de usuario e obrigatorio.")
        @Size(min = 3, max = 50, message = "O nome de usuario deve ter entre 3 e 50 caracteres.")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "O nome de usuario aceita apenas letras, numeros e os simbolos . _ -")
        String username) {
}
