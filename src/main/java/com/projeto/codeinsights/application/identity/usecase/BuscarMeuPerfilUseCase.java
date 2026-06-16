package com.projeto.codeinsights.application.identity.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.identity.dto.MeuPerfilDTO;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BuscarMeuPerfilUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public MeuPerfilDTO execute(UUID usuarioId) {
        Usuario usuario = usuarioRepository.buscarPorId(usuarioId)
                .orElseThrow(() -> new NegocioException("Usuario nao encontrado."));

        return new MeuPerfilDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.getStatus(),
                usuario.isPerfilPublico(),
                usuario.getDataCriacao());
    }
}
