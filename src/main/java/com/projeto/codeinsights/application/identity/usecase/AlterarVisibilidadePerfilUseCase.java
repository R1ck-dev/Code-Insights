package com.projeto.codeinsights.application.identity.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.identity.dto.AlterarVisibilidadePerfilInput;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlterarVisibilidadePerfilUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void execute(AlterarVisibilidadePerfilInput input) {
        Usuario usuario = usuarioRepository.buscarPorId(input.usuarioId())
                .orElseThrow(() -> new NegocioException("Usuario nao encontrado."));

        if (input.publico()) {
            usuario.tornarPerfilPublico();
        } else {
            usuario.tornarPerfilPrivado();
        }

        usuarioRepository.salvar(usuario);
    }
}
