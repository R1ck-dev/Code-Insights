package com.projeto.codeinsights.infrastructure.persistence.identity.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.identity.model.TokenVerificacao;
import com.projeto.codeinsights.domain.identity.port.TokenVerificacaoRepository;
import com.projeto.codeinsights.infrastructure.persistence.identity.entity.TokenVerificacaoJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.identity.mapper.TokenVerificacaoMapper;
import com.projeto.codeinsights.infrastructure.persistence.identity.repository.SpringDataTokenVerificacaoRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TokenVerificacaoRepositoryAdapter implements TokenVerificacaoRepository {

    private final SpringDataTokenVerificacaoRepository jpaRepository;
    private final TokenVerificacaoMapper mapper;

    @Override
    public TokenVerificacao salvar(TokenVerificacao token) {
        TokenVerificacaoJpaEntity entity = mapper.toEntity(token);
        TokenVerificacaoJpaEntity salvo = jpaRepository.save(entity);
        return mapper.toDomain(salvo);
    }

    @Override
    public Optional<TokenVerificacao> buscarPorToken(String token) {
        return jpaRepository.findByToken(token).map(mapper::toDomain);
    }
}
