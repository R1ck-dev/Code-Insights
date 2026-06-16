package com.projeto.codeinsights.domain.identity.port;

import com.projeto.codeinsights.domain.identity.model.Usuario;

public interface TokenServicePort {
    String gerarToken(Usuario usuario);

    String obterIdDoUsuario(String token);

    String obterRoleDoUsuario(String token);
}
