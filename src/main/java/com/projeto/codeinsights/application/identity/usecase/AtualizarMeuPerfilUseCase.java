package com.projeto.codeinsights.application.identity.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.identity.dto.AtualizarMeuPerfilInput;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtualizarMeuPerfilUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void execute(AtualizarMeuPerfilInput input) {
        Usuario usuario = usuarioRepository.buscarPorId(input.usuarioId())
                .orElseThrow(() -> new NegocioException("Usuario nao encontrado."));

        String novoUsername = input.username() != null ? input.username().trim() : null;

        boolean mudouUsername = novoUsername != null && !novoUsername.equalsIgnoreCase(usuario.getUsername());
        if (mudouUsername && usuarioRepository.existePorUsername(novoUsername)) {
            throw new NegocioException("Este nome de usuario ja esta em uso.");
        }

        usuario.atualizarUsername(input.username());
        usuarioRepository.salvar(usuario);
    }
}
