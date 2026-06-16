package com.projeto.codeinsights.application.identity.usecase;

import org.springframework.stereotype.Service;

import com.projeto.codeinsights.application.identity.dto.LoginInput;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.AuthenticationPort;
import com.projeto.codeinsights.domain.identity.port.TokenServicePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutenticarUsuarioUseCase {

    private final AuthenticationPort authenticationPort;
    private final TokenServicePort tokenServicePort;

    public String execute(LoginInput input) {
        Usuario usuarioAutenticado = authenticationPort.autenticar(input.email(), input.password());
        return tokenServicePort.gerarToken(usuarioAutenticado);
    }
}
