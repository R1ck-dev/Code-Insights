package com.projeto.codeinsights.infrastructure.persistence.knowledge.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.ResultadoMetricaJpaEntity;

@Repository
public interface SpringDataResultadoMetricaRepository extends JpaRepository<ResultadoMetricaJpaEntity, UUID> {

    List<ResultadoMetricaJpaEntity> findByResolucaoId(UUID resolucaoId);

    /**
     * Bulk delete imediato: executa o DELETE na hora, antes dos INSERTs da reanalise.
     * Um delete derivado apenas enfileiraria as remocoes, e o Hibernate emite INSERTs
     * antes de DELETEs no flush, colidindo com a UNIQUE(resolucao_id, tipo).
     */
    @Modifying
    @Query("delete from ResultadoMetricaJpaEntity r where r.resolucao.id = :resolucaoId")
    void deleteByResolucaoId(@Param("resolucaoId") UUID resolucaoId);
}
