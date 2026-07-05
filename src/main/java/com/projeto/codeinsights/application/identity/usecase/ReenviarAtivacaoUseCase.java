package com.projeto.codeinsights.application.identity.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.identity.dto.ReenviarAtivacaoInput;
import com.projeto.codeinsights.domain.identity.enums.StatusConta;
import com.projeto.codeinsights.domain.identity.enums.TipoToken;
import com.projeto.codeinsights.domain.identity.model.TokenVerificacao;
import com.projeto.codeinsights.domain.identity.port.EmailSenderPort;
import com.projeto.codeinsights.domain.identity.port.TokenVerificacaoRepository;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReenviarAtivacaoUseCase {

    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacaoRepository tokenVerificacaoRepository;
    private final EmailSenderPort emailSenderPort;

    @Transactional
    public void execute(ReenviarAtivacaoInput input) {
        usuarioRepository.buscarPorEmail(input.email()).ifPresent(usuario -> {
            // So reemite se a conta ainda estiver pendente; resposta neutra nos demais casos.
            if (usuario.getStatus() != StatusConta.PENDENTE_VERIFICACAO) {
                return;
            }
            TokenVerificacao token = new TokenVerificacao(usuario, TipoToken.ATIVACAO);
            tokenVerificacaoRepository.salvar(token);
            emailSenderPort.enviarEmailAtivacao(usuario.getEmail(), usuario.getUsername(), token.getToken());
        });
    }
}
