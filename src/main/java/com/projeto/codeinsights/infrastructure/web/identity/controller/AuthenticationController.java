package com.projeto.codeinsights.infrastructure.web.identity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.codeinsights.application.identity.dto.LoginInput;
import com.projeto.codeinsights.application.identity.usecase.AutenticarUsuarioUseCase;
import com.projeto.codeinsights.infrastructure.web.identity.dto.LoginRequest;
import com.projeto.codeinsights.infrastructure.web.identity.dto.TokenResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AutenticarUsuarioUseCase autenticarUsuarioUseCase;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginInput input = new LoginInput(request.email(), request.password());
        String tokenJwt = autenticarUsuarioUseCase.execute(input);
        return ResponseEntity.ok(new TokenResponse(tokenJwt));
    }
}
