package com.projeto.codeinsights.infrastructure.web.pesquisa.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo da revelacao de identidade: o codigo que aparece na tela e no CSV. */
public record PseudonimoRequest(
        @NotBlank(message = "O pseudonimo e obrigatorio.")
        String pseudonimo) {
}
