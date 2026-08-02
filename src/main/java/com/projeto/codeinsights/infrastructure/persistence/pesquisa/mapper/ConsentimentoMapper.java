package com.projeto.codeinsights.infrastructure.persistence.pesquisa.mapper;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.pesquisa.model.ConsentimentoDePesquisa;
import com.projeto.codeinsights.infrastructure.persistence.pesquisa.entity.ConsentimentoJpaEntity;

@Component
public class ConsentimentoMapper {

    public ConsentimentoJpaEntity toEntity(ConsentimentoDePesquisa consentimento) {
        ConsentimentoJpaEntity entity = new ConsentimentoJpaEntity();
        entity.setId(consentimento.getId());
        entity.setParticipanteId(consentimento.getParticipanteId());
        entity.setVersaoDoTermo(consentimento.getVersaoDoTermo());
        entity.setDecisao(consentimento.getDecisao());
        entity.setRegistradoEm(consentimento.getRegistradoEm());
        return entity;
    }

    public ConsentimentoDePesquisa toDomain(ConsentimentoJpaEntity entity) {
        return new ConsentimentoDePesquisa(
                entity.getId(),
                entity.getParticipanteId(),
                entity.getVersaoDoTermo(),
                entity.getDecisao(),
                entity.getRegistradoEm());
    }
}
