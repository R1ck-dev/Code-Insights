package com.projeto.codeinsights.infrastructure.web.identity.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.codeinsights.application.identity.dto.EsqueciSenhaInput;
import com.projeto.codeinsights.application.identity.dto.LoginInput;
import com.projeto.codeinsights.application.identity.dto.RedefinirSenhaInput;
import com.projeto.codeinsights.application.identity.usecase.AutenticarUsuarioUseCase;
import com.projeto.codeinsights.application.identity.usecase.RedefinirSenhaUseCase;
import com.projeto.codeinsights.application.identity.usecase.SolicitarRedefinicaoSenhaUseCase;
import com.projeto.codeinsights.infrastructure.web.identity.dto.EsqueciSenhaRequest;
import com.projeto.codeinsights.infrastructure.web.identity.dto.LoginRequest;
import com.projeto.codeinsights.infrastructure.web.identity.dto.RedefinirSenhaRequest;
import com.projeto.codeinsights.infrastructure.web.identity.dto.TokenResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AutenticarUsuarioUseCase autenticarUsuarioUseCase;
    private final SolicitarRedefinicaoSenhaUseCase solicitarRedefinicaoSenhaUseCase;
    private final RedefinirSenhaUseCase redefinirSenhaUseCase;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginInput input = new LoginInput(request.email(), request.password());
        String tokenJwt = autenticarUsuarioUseCase.execute(input);
        return ResponseEntity.ok(new TokenResponse(tokenJwt));
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<Map<String, String>> esqueciSenha(@RequestBody @Valid EsqueciSenhaRequest request) {
        solicitarRedefinicaoSenhaUseCase.execute(new EsqueciSenhaInput(request.email()));
        return ResponseEntity.ok(Map.of(
                "mensagem", "Se o e-mail estiver cadastrado, voce recebera um link para redefinir a senha."));
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Map<String, String>> redefinirSenha(@RequestBody @Valid RedefinirSenhaRequest request) {
        redefinirSenhaUseCase.execute(new RedefinirSenhaInput(request.token(), request.novaSenha()));
        return ResponseEntity.ok(Map.of(
                "mensagem", "Senha redefinida com sucesso! Voce ja pode realizar o login."));
    }
}
