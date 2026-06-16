package com.projeto.codeinsights.domain.identity.port;

import com.projeto.codeinsights.domain.identity.model.Usuario;

public interface AuthenticationPort {
    Usuario autenticar(String email, String rawPassword);
}
