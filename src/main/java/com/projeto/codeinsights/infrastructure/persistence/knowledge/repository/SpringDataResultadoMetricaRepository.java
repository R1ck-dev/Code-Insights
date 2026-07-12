package com.projeto.codeinsights.infrastructure.persistence.knowledge.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.ResultadoMetricaJpaEntity;

@Repository
public interface SpringDataResultadoMetricaRepository extends JpaRepository<ResultadoMetricaJpaEntity, UUID> {

    List<ResultadoMetricaJpaEntity> findByResolucaoId(UUID resolucaoId);

    // Lote: as metricas de um tipo para varias resolucoes de uma vez (uma consulta, sem N+1).
    // Ha no maximo 1 resultado por (resolucao, tipo) — UNIQUE(resolucao_id, tipo) —, entao o
    // retorno tem no maximo uma linha por resolucao e pode virar mapa direto.
    List<ResultadoMetricaJpaEntity> findByResolucaoIdInAndTipo(List<UUID> resolucaoIds, TipoMetrica tipo);

    /**
     * Bulk delete imediato: executa o DELETE na hora, antes dos INSERTs da reanalise.
     * Um delete derivado apenas enfileiraria as remocoes, e o Hibernate emite INSERTs
     * antes de DELETEs no flush, colidindo com a UNIQUE(resolucao_id, tipo).
     */
    @Modifying
    @Query("delete from ResultadoMetricaJpaEntity r where r.resolucao.id = :resolucaoId")
    void deleteByResolucaoId(@Param("resolucaoId") UUID resolucaoId);

    // Distribuicao por rotulo/ordinal de um tipo de metrica entre as resolucoes do autor.
    @Query("""
            select m.rotulo, m.valor, count(m)
            from ResultadoMetricaJpaEntity m
            where m.resolucao.autor.id = :autorId and m.tipo = :tipo
            group by m.rotulo, m.valor
            order by m.valor
            """)
    List<Object[]> contarPorRotulo(@Param("autorId") UUID autorId, @Param("tipo") TipoMetrica tipo);
}
