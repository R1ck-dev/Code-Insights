package com.projeto.codeinsights.infrastructure.persistence.knowledge.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.ResolucaoJpaEntity;

@Repository
public interface SpringDataResolucaoRepository extends JpaRepository<ResolucaoJpaEntity, UUID> {

    Page<ResolucaoJpaEntity> findByDesafioId(UUID desafioId, Pageable pageable);

    Page<ResolucaoJpaEntity> findByDesafioIdAndVisibilidade(
            UUID desafioId, Visibilidade visibilidade, Pageable pageable);

    long countByDesafioId(UUID desafioId);

    long countByDesafioIdAndVisibilidade(UUID desafioId, Visibilidade visibilidade);

    long countByAutorId(UUID autorId);

    long countByAutorIdAndAnalisadaTrue(UUID autorId);

    @Query("select avg(r.indiceAutonomiaIA) from ResolucaoJpaEntity r where r.autor.id = :autorId")
    Double mediaAutonomiaPorAutor(@Param("autorId") UUID autorId);

    // Serie temporal de submissao agrupada por :campo do date_trunc ('day'/'week'/'month'). Nativa (Postgres).
    // O left join traz a media da ordem de Big O (tempo) das resolucoes analisadas do periodo; ha no
    // maximo 1 metrica BIG_O_TEMPO por resolucao, entao a juncao nao infla a media de autonomia nem a contagem.
    @Query(value = """
            select extract(year  from date_trunc(:campo, r.submetida_em)) as ano,
                   extract(month from date_trunc(:campo, r.submetida_em)) as mes,
                   extract(day   from date_trunc(:campo, r.submetida_em)) as dia,
                   avg(r.indice_autonomia_ia)                             as media_autonomia,
                   count(*)                                               as total_resolucoes,
                   avg(m.valor)                                           as media_complexidade
            from resolucoes r
            left join resultados_metrica m
                   on m.resolucao_id = r.id and m.tipo = 'BIG_O_TEMPO' and m.valor >= 0
            where r.autor_id = :autorId
            group by 1, 2, 3
            order by 1, 2, 3
            """, nativeQuery = true)
    List<Object[]> evolucaoPorAutor(@Param("autorId") UUID autorId, @Param("campo") String campo);

    // Atividade recente: resolucoes + titulo do desafio + Big O de tempo (left join, pode faltar).
    // Limite aplicado via Pageable (PageRequest.of(0, limite)).
    @Query("""
            select r.id, d.id, d.titulo, r.linguagem, r.indiceAutonomiaIA, r.analisada,
                   m.rotulo, m.valor, r.submetidaEm
            from ResolucaoJpaEntity r
            join r.desafio d
            left join ResultadoMetricaJpaEntity m on m.resolucao = r and m.tipo = :tipo
            where r.autor.id = :autorId
            order by r.submetidaEm desc
            """)
    List<Object[]> atividadeRecentePorAutor(@Param("autorId") UUID autorId,
            @Param("tipo") TipoMetrica tipo, Pageable pageable);
}
