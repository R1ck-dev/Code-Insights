package com.projeto.codeinsights.infrastructure.web.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
        @NotBlank(message = "O token e obrigatorio.")
        String token,

        @NotBlank(message = "A senha e obrigatoria.")
        @Size(min = 8, message = "A senha deve ter no minimo 8 caracteres.")
        String novaSenha) {
}
