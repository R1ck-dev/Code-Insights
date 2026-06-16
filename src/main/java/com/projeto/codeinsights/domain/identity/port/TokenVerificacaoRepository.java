package com.projeto.codeinsights.domain.identity.port;

import java.util.Optional;

import com.projeto.codeinsights.domain.identity.model.TokenVerificacao;

public interface TokenVerificacaoRepository {
    TokenVerificacao salvar(TokenVerificacao token);

    Optional<TokenVerificacao> buscarPorToken(String token);
}
