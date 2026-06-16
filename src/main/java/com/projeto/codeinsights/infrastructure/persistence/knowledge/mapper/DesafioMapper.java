package com.projeto.codeinsights.infrastructure.persistence.knowledge.mapper;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.infrastructure.persistence.identity.entity.UsuarioJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.DesafioJpaEntity;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DesafioMapper {

    private final EntityManager entityManager;

    public DesafioJpaEntity toEntity(Desafio domain) {
        DesafioJpaEntity entity = new DesafioJpaEntity();
        entity.setId(domain.getId());
        entity.setAutor(entityManager.getReference(UsuarioJpaEntity.class, domain.getAutorId()));
        entity.setTitulo(domain.getTitulo());
        entity.setDescricao(domain.getDescricao());
        entity.setOrigemPlataforma(domain.getOrigemPlataforma());
        entity.setDificuldade(domain.getDificuldade());
        entity.setPublico(domain.isPublico());
        entity.setDataCriacao(domain.getDataCriacao());
        entity.setDataAtualizacao(domain.getDataAtualizacao());
        return entity;
    }

    public Desafio toDomain(DesafioJpaEntity entity) {
        return new Desafio(
                entity.getId(),
                entity.getAutor().getId(),
                entity.getTitulo(),
                entity.getDescricao(),
                entity.getOrigemPlataforma(),
                entity.getDificuldade(),
                entity.isPublico(),
                entity.getDataCriacao(),
                entity.getDataAtualizacao());
    }
}
