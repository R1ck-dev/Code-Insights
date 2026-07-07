package com.projeto.codeinsights.infrastructure.persistence.identity.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projeto.codeinsights.domain.identity.enums.StatusConta;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;
import com.projeto.codeinsights.infrastructure.persistence.identity.entity.UsuarioJpaEntity;

@Repository
public interface SpringDataUsuarioRepository extends JpaRepository<UsuarioJpaEntity, UUID> {
    Optional<UsuarioJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // Diretorio de portfolios: perfis publicos e ativos, exceto o proprio, filtrando
    // por username (filtro vazio = todos, pois "" esta contido em qualquer string).
    Page<UsuarioJpaEntity> findByVisibilidadePerfilAndStatusAndIdNotAndUsernameContainingIgnoreCase(
            Visibilidade visibilidadePerfil, StatusConta status, UUID id, String username, Pageable pageable);
}
