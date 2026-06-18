package com.projeto.codeinsights.infrastructure.persistence.knowledge.mapper;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.model.Snippet;
import com.projeto.codeinsights.infrastructure.persistence.identity.entity.UsuarioJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.SnippetJpaEntity;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SnippetMapper {

    private final EntityManager entityManager;

    public SnippetJpaEntity toEntity(Snippet domain) {
        SnippetJpaEntity entity = new SnippetJpaEntity();
        entity.setId(domain.getId());
        entity.setAutor(entityManager.getReference(UsuarioJpaEntity.class, domain.getAutorId()));
        entity.setCodigo(domain.getCodigo());
        entity.setDescricao(domain.getDescricao());
        entity.setCategoria(domain.getCategoria());
        entity.setCriadoEm(domain.getCriadoEm());
        return entity;
    }

    public Snippet toDomain(SnippetJpaEntity entity) {
        return new Snippet(
                entity.getId(),
                entity.getAutor().getId(),
                entity.getCodigo(),
                entity.getDescricao(),
                entity.getCategoria(),
                entity.getCriadoEm());
    }
}
