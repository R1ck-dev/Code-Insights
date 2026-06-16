package com.projeto.codeinsights.infrastructure.persistence.knowledge.mapper;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.model.Snippet;
import com.projeto.codeinsights.infrastructure.persistence.identity.entity.UsuarioJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.ResolucaoJpaEntity;
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
        if (domain.getResolucaoId() != null) {
            entity.setResolucao(entityManager.getReference(ResolucaoJpaEntity.class, domain.getResolucaoId()));
        } else {
            entity.setResolucao(null);
        }
        entity.setTitulo(domain.getTitulo());
        entity.setCodigo(domain.getCodigo());
        entity.setDescricao(domain.getDescricao());
        entity.setCategoriaConceito(domain.getCategoriaConceito());
        entity.setDataCriacao(domain.getDataCriacao());
        entity.setDataAtualizacao(domain.getDataAtualizacao());
        return entity;
    }

    public Snippet toDomain(SnippetJpaEntity entity) {
        return new Snippet(
                entity.getId(),
                entity.getAutor().getId(),
                entity.getResolucao() != null ? entity.getResolucao().getId() : null,
                entity.getTitulo(),
                entity.getCodigo(),
                entity.getDescricao(),
                entity.getCategoriaConceito(),
                entity.getDataCriacao(),
                entity.getDataAtualizacao());
    }
}
