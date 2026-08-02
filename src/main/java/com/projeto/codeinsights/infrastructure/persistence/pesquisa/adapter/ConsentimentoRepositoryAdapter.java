package com.projeto.codeinsights.infrastructure.persistence.pesquisa.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.pesquisa.model.ConsentimentoDePesquisa;
import com.projeto.codeinsights.domain.pesquisa.port.ConsentimentoRepository;
import com.projeto.codeinsights.infrastructure.persistence.pesquisa.entity.ConsentimentoJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.pesquisa.mapper.ConsentimentoMapper;
import com.projeto.codeinsights.infrastructure.persistence.pesquisa.repository.SpringDataConsentimentoRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConsentimentoRepositoryAdapter implements ConsentimentoRepository {

    private final SpringDataConsentimentoRepository springDataConsentimentoRepository;
    private final ConsentimentoMapper consentimentoMapper;

    @Override
    public ConsentimentoDePesquisa registrar(ConsentimentoDePesquisa consentimento) {
        ConsentimentoJpaEntity salvo = springDataConsentimentoRepository
                .save(consentimentoMapper.toEntity(consentimento));
        return consentimentoMapper.toDomain(salvo);
    }

    @Override
    public List<ConsentimentoDePesquisa> decisoesDaVersao(String versaoDoTermo) {
        return springDataConsentimentoRepository.findByVersaoDoTermoOrderByRegistradoEmAsc(versaoDoTermo)
                .stream()
                .map(consentimentoMapper::toDomain)
                .toList();
    }

    @Override
    public List<ConsentimentoDePesquisa> historicoDoParticipante(UUID participanteId) {
        return springDataConsentimentoRepository.findByParticipanteIdOrderByRegistradoEmAsc(participanteId)
                .stream()
                .map(consentimentoMapper::toDomain)
                .toList();
    }
}
