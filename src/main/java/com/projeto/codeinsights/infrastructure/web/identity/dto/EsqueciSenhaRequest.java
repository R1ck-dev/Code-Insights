package com.projeto.codeinsights.infrastructure.web.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EsqueciSenhaRequest(
        @NotBlank(message = "O e-mail e obrigatorio.")
        @Email(message = "Formato de e-mail invalido.")
        String email) {
}
