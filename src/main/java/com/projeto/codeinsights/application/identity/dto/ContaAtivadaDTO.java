package com.projeto.codeinsights.application.identity.dto;

import java.util.UUID;

import com.projeto.codeinsights.domain.identity.enums.StatusConta;

/** Retorno da ativacao manual de uma conta pelo administrador. */
public record ContaAtivadaDTO(
        UUID id,
        String username,
        String email,
        StatusConta status) {
}
