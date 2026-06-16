package com.projeto.codeinsights.infrastructure.persistence.identity.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projeto.codeinsights.infrastructure.persistence.identity.entity.TokenVerificacaoJpaEntity;

@Repository
public interface SpringDataTokenVerificacaoRepository extends JpaRepository<TokenVerificacaoJpaEntity, UUID> {
    @EntityGraph(attributePaths = "usuario")
    Optional<TokenVerificacaoJpaEntity> findByToken(String token);
}
