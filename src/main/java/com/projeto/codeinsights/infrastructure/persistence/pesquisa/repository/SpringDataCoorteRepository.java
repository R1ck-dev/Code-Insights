package com.projeto.codeinsights.infrastructure.persistence.pesquisa.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.ResolucaoJpaEntity;

/**
 * Leitura do corpus inteiro para a pesquisa. Le as tabelas do contexto {@code knowledge} —
 * inevitavel, o dado analisado e o mesmo — mas mora no proprio contexto porque a consulta pertence
 * a quem a consome. Este repositorio e <b>somente leitura</b>: escrita em resolucao continua sendo
 * exclusividade de {@code SpringDataResolucaoRepository}.
 */
@Repository
public interface SpringDataCoorteRepository extends JpaRepository<ResolucaoJpaEntity, UUID> {

    // Espelha a consulta da carta celeste sem o filtro de autor e com o autor como coluna. A ordem
    // do select e exatamente a ordem dos componentes de ResolucaoDaCoorte — a projecao e posicional,
    // e reordenar aqui sem reordenar la troca metrica por metrica em silencio.
    // Um left join por tipo: ha no maximo 1 resultado por (resolucao, tipo), garantido pela
    // UNIQUE (resolucao_id, tipo), entao as juncoes nao multiplicam linhas.
    @Query("""
            select r.id, r.autor.id, d.id, d.titulo, r.linguagem, r.indiceAutonomiaIA, r.analisada,
                   mt.rotulo, mt.valor, mt.confianca,
                   me.rotulo, me.valor, me.confianca,
                   mc.valor,
                   r.submetidaEm
            from ResolucaoJpaEntity r
            join r.desafio d
            left join ResultadoMetricaJpaEntity mt on mt.resolucao = r and mt.tipo = :tipoTempo
            left join ResultadoMetricaJpaEntity me on me.resolucao = r and me.tipo = :tipoEspaco
            left join ResultadoMetricaJpaEntity mc on mc.resolucao = r and mc.tipo = :tipoCiclomatica
            where r.autor.id in :autores
            order by r.submetidaEm asc
            """)
    List<Object[]> coorte(@Param("tipoTempo") TipoMetrica tipoTempo,
            @Param("tipoEspaco") TipoMetrica tipoEspaco,
            @Param("tipoCiclomatica") TipoMetrica tipoCiclomatica,
            @Param("autores") Set<UUID> autores);

    @Query("select distinct r.autor.id from ResolucaoJpaEntity r")
    Set<UUID> autoresComResolucao();
}
