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

    // Carta celeste: TODAS as resolucoes do autor + titulo do desafio + as tres metricas, numa unica
    // query (um left join por tipo — ha no maximo 1 resultado por (resolucao, tipo), entao as juncoes
    // nao multiplicam linhas). Resolucao nao analisada casa com nenhuma metrica e vem com nulos.
    @Query("""
            select r.id, d.id, d.titulo, r.linguagem, r.indiceAutonomiaIA, r.analisada,
                   r.visibilidade, r.submetidaEm,
                   mt.rotulo, mt.valor, mt.confianca,
                   me.rotulo, me.valor,
                   mc.valor
            from ResolucaoJpaEntity r
            join r.desafio d
            left join ResultadoMetricaJpaEntity mt on mt.resolucao = r and mt.tipo = :tipoTempo
            left join ResultadoMetricaJpaEntity me on me.resolucao = r and me.tipo = :tipoEspaco
            left join ResultadoMetricaJpaEntity mc on mc.resolucao = r and mc.tipo = :tipoCiclomatica
            where r.autor.id = :autorId
            order by r.submetidaEm asc
            """)
    List<Object[]> pontosCartaPorAutor(@Param("autorId") UUID autorId,
            @Param("tipoTempo") TipoMetrica tipoTempo,
            @Param("tipoEspaco") TipoMetrica tipoEspaco,
            @Param("tipoCiclomatica") TipoMetrica tipoCiclomatica);

    // Reanalise do corpus: apenas os ids. Trazer o codigo-fonte de todas as resolucoes de uma vez
    // nao escala; cada uma e recarregada e reprocessada na sua propria transacao. Ordem por data de
    // submissao para a passada ser reproduzivel.
    @Query("select r.id from ResolucaoJpaEntity r order by r.submetidaEm asc")
    List<UUID> listarTodosIds();

    @Query("select r.id from ResolucaoJpaEntity r where r.autor.id = :autorId order by r.submetidaEm asc")
    List<UUID> listarIdsPorAutor(@Param("autorId") UUID autorId);
}
