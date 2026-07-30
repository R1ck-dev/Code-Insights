package com.projeto.codeinsights.application.identity.dto;

import java.time.LocalDateTime;

/**
 * Token de redefinicao de senha gerado pelo administrador, para entrega fora da
 * aplicacao. O caso de uso devolve o token cru; montar a URL e responsabilidade da
 * camada web, que e quem conhece o endereco publico do front.
 */
public record TokenRedefinicaoDTO(
        String username,
        String email,
        String token,
        LocalDateTime expiraEm) {
}
