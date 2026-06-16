package com.projeto.codeinsights.application.identity.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.domain.identity.enums.TipoToken;
import com.projeto.codeinsights.domain.identity.model.TokenVerificacao;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.TokenVerificacaoRepository;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtivarContaUseCase {

    private final TokenVerificacaoRepository tokenVerificacaoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void execute(String token) {
        TokenVerificacao tokenVerificacao = tokenVerificacaoRepository.buscarPorToken(token)
                .orElseThrow(() -> new NegocioException("Token de verificacao invalido ou nao encontrado."));

        if (tokenVerificacao.getTipo() != TipoToken.ATIVACAO) {
            throw new NegocioException("Token invalido para ativacao de conta.");
        }

        tokenVerificacao.validar();

        Usuario usuario = tokenVerificacao.getUsuario();
        usuario.ativarConta();
        tokenVerificacao.marcarComoUtilizado();

        usuarioRepository.salvar(usuario);
        tokenVerificacaoRepository.salvar(tokenVerificacao);
    }
}
