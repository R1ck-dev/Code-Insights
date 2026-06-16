package com.projeto.codeinsights.infrastructure.persistence.knowledge.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.SnippetJpaEntity;

@Repository
public interface SpringDataSnippetRepository extends JpaRepository<SnippetJpaEntity, UUID> {

    Page<SnippetJpaEntity> findByAutorId(UUID autorId, Pageable pageable);
}
