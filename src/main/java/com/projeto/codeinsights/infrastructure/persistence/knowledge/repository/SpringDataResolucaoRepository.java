package com.projeto.codeinsights.infrastructure.persistence.knowledge.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.ResolucaoJpaEntity;

@Repository
public interface SpringDataResolucaoRepository extends JpaRepository<ResolucaoJpaEntity, UUID> {

    Page<ResolucaoJpaEntity> findByDesafioId(UUID desafioId, Pageable pageable);

    long countByDesafioId(UUID desafioId);
}
