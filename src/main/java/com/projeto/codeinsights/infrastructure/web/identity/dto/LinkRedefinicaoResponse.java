package com.projeto.codeinsights.infrastructure.web.identity.dto;

import java.time.LocalDateTime;

/**
 * Link pronto para o administrador copiar e entregar ao aluno. Especifico de web porque a URL
 * so existe nesta camada: o caso de uso produz o token, quem sabe o endereco publico do front
 * e a configuracao {@code app.web.base-url}.
 */
public record LinkRedefinicaoResponse(
        String username,
        String email,
        String link,
        LocalDateTime expiraEm) {
}
