package com.projeto.codeinsights.infrastructure.persistence.identity.mapper;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.identity.model.TokenVerificacao;
import com.projeto.codeinsights.infrastructure.persistence.identity.entity.TokenVerificacaoJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.identity.entity.UsuarioJpaEntity;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TokenVerificacaoMapper {

    private final EntityManager entityManager;
    private final UsuarioMapper usuarioMapper;

    public TokenVerificacaoJpaEntity toEntity(TokenVerificacao domain) {
        TokenVerificacaoJpaEntity entity = new TokenVerificacaoJpaEntity();
        entity.setId(domain.getId());
        entity.setToken(domain.getToken());
        entity.setDataExpiracao(domain.getDataExpiracao());
        entity.setUtilizado(domain.isUtilizado());
        entity.setTipo(domain.getTipo());
        entity.setUsuario(entityManager.getReference(UsuarioJpaEntity.class, domain.getUsuario().getId()));
        return entity;
    }

    public TokenVerificacao toDomain(TokenVerificacaoJpaEntity entity) {
        return new TokenVerificacao(
                entity.getId(),
                usuarioMapper.toDomain(entity.getUsuario()),
                entity.getToken(),
                entity.getDataExpiracao(),
                entity.isUtilizado(),
                entity.getTipo());
    }
}
