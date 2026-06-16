package com.projeto.codeinsights.infrastructure.persistence.identity.mapper;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.infrastructure.persistence.identity.entity.UsuarioJpaEntity;

@Component
public class UsuarioMapper {

    public UsuarioJpaEntity toEntity(Usuario usuario) {
        UsuarioJpaEntity entity = new UsuarioJpaEntity();
        entity.setId(usuario.getId());
        entity.setUsername(usuario.getUsername());
        entity.setEmail(usuario.getEmail());
        entity.setSenhaHash(usuario.getSenhaHash());
        entity.setPerfilPublico(usuario.isPerfilPublico());
        entity.setStatus(usuario.getStatus());
        entity.setRole(usuario.getRole());
        entity.setDataCriacao(usuario.getDataCriacao());
        entity.setDataAtualizacao(usuario.getDataAtualizacao());
        return entity;
    }

    public Usuario toDomain(UsuarioJpaEntity entity) {
        return new Usuario(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getSenhaHash(),
                entity.isPerfilPublico(),
                entity.getStatus(),
                entity.getRole(),
                entity.getDataCriacao(),
                entity.getDataAtualizacao());
    }
}
