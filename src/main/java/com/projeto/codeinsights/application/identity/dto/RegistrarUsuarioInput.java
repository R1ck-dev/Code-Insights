package com.projeto.codeinsights.application.identity.dto;

public record RegistrarUsuarioInput(
        String username,
        String email,
        String password) {
}
