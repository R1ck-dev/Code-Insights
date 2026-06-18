package com.projeto.codeinsights.infrastructure.persistence.knowledge.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projeto.codeinsights.domain.shared.enums.Visibilidade;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.DesafioJpaEntity;

@Repository
public interface SpringDataDesafioRepository extends JpaRepository<DesafioJpaEntity, UUID> {

    Page<DesafioJpaEntity> findByAutorId(UUID autorId, Pageable pageable);

    Page<DesafioJpaEntity> findByAutorIdAndVisibilidade(UUID autorId, Visibilidade visibilidade, Pageable pageable);
}
