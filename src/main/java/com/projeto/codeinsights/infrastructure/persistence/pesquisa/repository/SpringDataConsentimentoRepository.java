package com.projeto.codeinsights.infrastructure.persistence.pesquisa.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projeto.codeinsights.infrastructure.persistence.pesquisa.entity.ConsentimentoJpaEntity;

/**
 * As duas leituras vem <b>ordenadas por instante</b> de proposito. A regra de qual decisao vale e do
 * dominio, mas entregar o log fora de ordem convidaria quem for ler a tratar "a ultima linha" como
 * "a decisao vigente" — que e verdade so por acidente do plano de execucao.
 */
@Repository
public interface SpringDataConsentimentoRepository extends JpaRepository<ConsentimentoJpaEntity, UUID> {

    List<ConsentimentoJpaEntity> findByVersaoDoTermoOrderByRegistradoEmAsc(String versaoDoTermo);

    List<ConsentimentoJpaEntity> findByParticipanteIdOrderByRegistradoEmAsc(UUID participanteId);
}
