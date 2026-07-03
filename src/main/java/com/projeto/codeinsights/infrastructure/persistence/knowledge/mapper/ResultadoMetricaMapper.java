package com.projeto.codeinsights.infrastructure.persistence.knowledge.mapper;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.ResolucaoJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.ResultadoMetricaJpaEntity;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResultadoMetricaMapper {

    private final EntityManager entityManager;

    public ResultadoMetricaJpaEntity toEntity(ResultadoMetrica domain) {
        ResultadoMetricaJpaEntity entity = new ResultadoMetricaJpaEntity();
        entity.setId(domain.getId());
        entity.setResolucao(entityManager.getReference(ResolucaoJpaEntity.class, domain.getResolucaoId()));
        entity.setTipo(domain.getTipo());
        entity.setValor(domain.getValor());
        entity.setRotulo(domain.getRotulo());
        entity.setDetalhe(domain.getDetalhe());
        entity.setAnalisadoEm(domain.getAnalisadoEm());
        return entity;
    }

    public ResultadoMetrica toDomain(ResultadoMetricaJpaEntity entity) {
        return new ResultadoMetrica(
                entity.getId(),
                entity.getResolucao().getId(),
                entity.getTipo(),
                entity.getValor(),
                entity.getRotulo(),
                entity.getDetalhe(),
                entity.getAnalisadoEm());
    }
}
