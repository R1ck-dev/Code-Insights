package com.projeto.codeinsights.application.identity.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.identity.dto.UsuarioPublicoDTO;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BuscarUsuarioPublicoUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public UsuarioPublicoDTO execute(UUID usuarioId) {
        Usuario usuario = usuarioRepository.buscarPorId(usuarioId)
                .orElseThrow(() -> new NegocioException("Usuario nao encontrado."));

        // A visibilidade do perfil e respeitada: perfis privados nao sao expostos
        // publicamente. O proprio dono ve seus dados via GET /api/usuarios/me.
        if (usuario.getVisibilidadePerfil() != Visibilidade.PUBLICO) {
            throw new NegocioException("Perfil nao encontrado ou e privado.");
        }

        return new UsuarioPublicoDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getVisibilidadePerfil());
    }
}
