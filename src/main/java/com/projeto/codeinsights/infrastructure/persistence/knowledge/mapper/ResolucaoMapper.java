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
        entity.setLinguagem(domain.getLinguagem());
        entity.setCodigoFonte(domain.getCodigoFonte());
        entity.setIndiceAutonomiaIa(domain.getIndiceAutonomiaIa());
        entity.setComplexidadeTempo(domain.getComplexidadeTempo());
        entity.setComplexidadeEspaco(domain.getComplexidadeEspaco());
        entity.setComplexidadeCiclomatica(domain.getComplexidadeCiclomatica());
        entity.setDataCriacao(domain.getDataCriacao());
        entity.setDataAtualizacao(domain.getDataAtualizacao());
        return entity;
    }

    public Resolucao toDomain(ResolucaoJpaEntity entity) {
        return new Resolucao(
                entity.getId(),
                entity.getDesafio().getId(),
                entity.getAutor().getId(),
                entity.getLinguagem(),
                entity.getCodigoFonte(),
                entity.getIndiceAutonomiaIa(),
                entity.getComplexidadeTempo(),
                entity.getComplexidadeEspaco(),
                entity.getComplexidadeCiclomatica(),
                entity.getDataCriacao(),
                entity.getDataAtualizacao());
    }
}
