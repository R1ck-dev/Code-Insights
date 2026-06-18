package com.projeto.codeinsights.infrastructure.persistence.knowledge.mapper;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.infrastructure.persistence.identity.entity.UsuarioJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.DesafioJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.ResolucaoJpaEntity;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResolucaoMapper {

    private final EntityManager entityManager;

    public ResolucaoJpaEntity toEntity(Resolucao domain) {
        ResolucaoJpaEntity entity = new ResolucaoJpaEntity();
        entity.setId(domain.getId());
        entity.setDesafio(entityManager.getReference(DesafioJpaEntity.class, domain.getDesafioId()));
        entity.setAutor(entityManager.getReference(UsuarioJpaEntity.class, domain.getAutorId()));
        entity.setCodigoFonte(domain.getCodigoFonte());
        entity.setLinguagem(domain.getLinguagem());
        entity.setIndiceAutonomiaIA(domain.getIndiceAutonomiaIA());
        entity.setDescricaoApoioIA(domain.getDescricaoApoioIA());
        entity.setVisibilidade(domain.getVisibilidade());
        entity.setAnalisada(domain.isAnalisada());
        entity.setSubmetidaEm(domain.getSubmetidaEm());
        return entity;
    }

    public Resolucao toDomain(ResolucaoJpaEntity entity) {
        return new Resolucao(
                entity.getId(),
                entity.getAutor().getId(),
                entity.getDesafio().getId(),
                entity.getCodigoFonte(),
                entity.getLinguagem(),
                entity.getIndiceAutonomiaIA(),
                entity.getDescricaoApoioIA(),
                entity.getVisibilidade(),
                entity.isAnalisada(),
                entity.getSubmetidaEm());
    }
}
