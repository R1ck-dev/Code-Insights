package com.projeto.codeinsights.application.identity.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.identity.dto.RedefinirSenhaInput;
import com.projeto.codeinsights.domain.identity.enums.TipoToken;
import com.projeto.codeinsights.domain.identity.model.TokenVerificacao;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.PasswordEncoderPort;
import com.projeto.codeinsights.domain.identity.port.TokenVerificacaoRepository;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedefinirSenhaUseCase {

    private final TokenVerificacaoRepository tokenVerificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoderPort passwordEncoderPort;

    @Transactional
    public void execute(RedefinirSenhaInput input) {
        TokenVerificacao tokenVerificacao = tokenVerificacaoRepository.buscarPorToken(input.token())
                .orElseThrow(() -> new NegocioException("Token de redefinicao invalido ou nao encontrado."));

        if (tokenVerificacao.getTipo() != TipoToken.REDEFINICAO_SENHA) {
            throw new NegocioException("Token invalido para redefinicao de senha.");
        }

        tokenVerificacao.validar();

        Usuario usuario = tokenVerificacao.getUsuario();
        usuario.definirSenha(passwordEncoderPort.encode(input.novaSenha()));
        tokenVerificacao.marcarComoUtilizado();

        usuarioRepository.salvar(usuario);
        tokenVerificacaoRepository.salvar(tokenVerificacao);
    }
}
