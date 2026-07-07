package com.projeto.codeinsights.infrastructure.persistence.knowledge.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.projeto.codeinsights.domain.knowledge.enums.CategoriaConceito;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.SnippetJpaEntity;

@Repository
public interface SpringDataSnippetRepository extends JpaRepository<SnippetJpaEntity, UUID> {

    Page<SnippetJpaEntity> findByAutorId(UUID autorId, Pageable pageable);

    Page<SnippetJpaEntity> findByAutorIdAndCategoria(UUID autorId, CategoriaConceito categoria, Pageable pageable);

    Page<SnippetJpaEntity> findByAutorIdAndDesafioId(UUID autorId, UUID desafioId, Pageable pageable);

    long countByAutorId(UUID autorId);

    @Query("select count(distinct s.categoria) from SnippetJpaEntity s where s.autor.id = :autorId")
    long contarCategoriasPorAutor(@Param("autorId") UUID autorId);
}
