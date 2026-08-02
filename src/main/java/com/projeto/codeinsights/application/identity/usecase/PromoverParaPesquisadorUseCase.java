package com.projeto.codeinsights.application.identity.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.identity.dto.PapelAlteradoDTO;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

/**
 * Da a alguem acesso a area de pesquisa. O comportamento ja existia no dominio
 * ({@code Usuario.promoverParaPesquisador()}) mas ninguem o chamava — na pratica, era impossivel
 * criar uma conta de pesquisador sem mexer no banco a mao.
 * <p>
 * <b>O papel viaja dentro do JWT</b>, entao promover alguem nao afeta o token que a pessoa ja tem:
 * o acesso novo so vale apos novo login. Nao ha revogacao nem refresh de token na plataforma.
 */
@Service
@RequiredArgsConstructor
public class PromoverParaPesquisadorUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public PapelAlteradoDTO execute(String email) {
        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new NegocioException("Nenhum usuario cadastrado com o e-mail informado."));

        usuario.promoverParaPesquisador();
        Usuario salvo = usuarioRepository.salvar(usuario);

        return new PapelAlteradoDTO(salvo.getId(), salvo.getUsername(), salvo.getEmail(), salvo.getRole());
    }
}
