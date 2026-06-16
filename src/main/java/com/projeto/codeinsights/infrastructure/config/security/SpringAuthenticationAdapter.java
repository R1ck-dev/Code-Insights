package com.projeto.codeinsights.infrastructure.config.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.identity.enums.StatusUsuario;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.AuthenticationPort;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SpringAuthenticationAdapter implements AuthenticationPort {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Usuario autenticar(String email, String rawPassword) {
        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new NegocioException("Credenciais invalidas."));

        if (!passwordEncoder.matches(rawPassword, usuario.getSenhaHash())) {
            throw new NegocioException("Credenciais invalidas.");
        }

        if (usuario.getStatus() != StatusUsuario.ATIVO) {
            throw new NegocioException("Conta pendente. Por favor, verifique o seu e-mail.");
        }

        return usuario;
    }
}
