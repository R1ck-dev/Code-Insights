package com.projeto.codeinsights.infrastructure.web.identity.dto;

public record TokenResponse(
        String token,
        String tipo) {

    public TokenResponse(String token) {
        this(token, "Bearer");
    }
}
