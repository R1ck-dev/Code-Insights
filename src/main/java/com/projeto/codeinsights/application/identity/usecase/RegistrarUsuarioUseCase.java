package com.projeto.codeinsights.application.identity.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.identity.dto.RegistrarUsuarioInput;
import com.projeto.codeinsights.domain.identity.enums.TipoToken;
import com.projeto.codeinsights.domain.identity.model.TokenVerificacao;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.EmailSenderPort;
import com.projeto.codeinsights.domain.identity.port.PasswordEncoderPort;
import com.projeto.codeinsights.domain.identity.port.TokenVerificacaoRepository;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenVerificacaoRepository tokenVerificacaoRepository;
    private final EmailSenderPort emailSenderPort;

    @Transactional
    public void execute(RegistrarUsuarioInput input) {
        if (usuarioRepository.existePorEmail(input.email())) {
            throw new NegocioException("Ja existe um usuario registrado com este e-mail.");
        }
        if (usuarioRepository.existePorUsername(input.username())) {
            throw new NegocioException("Este nome de usuario ja esta em uso.");
        }

        String hash = passwordEncoderPort.encode(input.password());

        Usuario novoUsuario = new Usuario(null, input.username(), input.email(), hash);
        Usuario usuarioSalvo = usuarioRepository.salvar(novoUsuario);

        TokenVerificacao token = new TokenVerificacao(usuarioSalvo, TipoToken.ATIVACAO);
        tokenVerificacaoRepository.salvar(token);

        emailSenderPort.enviarEmailAtivacao(usuarioSalvo.getEmail(), usuarioSalvo.getUsername(), token.getToken());
    }
}
