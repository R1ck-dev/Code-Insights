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
        entity.setRole(usuario.getRole());
        entity.setVisibilidadePerfil(usuario.getVisibilidadePerfil());
        entity.setStatus(usuario.getStatus());
        entity.setCriadoEm(usuario.getCriadoEm());
        entity.setAtualizadoEm(usuario.getAtualizadoEm());
        return entity;
    }

    public Usuario toDomain(UsuarioJpaEntity entity) {
        return new Usuario(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getSenhaHash(),
                entity.getRole(),
                entity.getVisibilidadePerfil(),
                entity.getStatus(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm());
    }
}
