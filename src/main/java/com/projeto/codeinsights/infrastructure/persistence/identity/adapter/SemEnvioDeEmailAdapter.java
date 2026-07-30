package com.projeto.codeinsights.infrastructure.persistence.identity.adapter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.identity.port.EmailSenderPort;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementacao usada quando a plataforma roda <b>sem provedor de e-mail</b>.
 * <p>
 * Nao e um adapter "de mentira" para testes: e o modo real do ambiente publico atual. A combinacao
 * hospedagem gratuita + sem dominio proprio nao tem saida de e-mail viavel — o Render bloqueia as
 * portas SMTP, Brevo e Resend exigem dominio verificado para enviar a terceiros, e o SMTP2GO recusa
 * cadastro com e-mail gratuito. Em vez de manter um canal que falha, a plataforma assume que nao ha
 * canal: o registro ativa a conta imediatamente e a recuperacao de acesso passa pelas rotas
 * administrativas.
 * <p>
 * Os metodos de envio lancam excecao porque, com {@link #habilitado()} devolvendo {@code false},
 * nenhum caso de uso deveria chegar aqui. Se chegar, e um fluxo que esqueceu de checar a capacidade
 * — falhar alto e melhor do que descartar o e-mail em silencio e deixar o aluno esperando um link
 * que nunca foi enviado.
 */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "nenhum")
@Slf4j
public class SemEnvioDeEmailAdapter implements EmailSenderPort {

    @Override
    public boolean habilitado() {
        return false;
    }

    @Override
    public void enviarEmailAtivacao(String destinatario, String nome, String token) {
        throw naoConfigurado("ativacao de conta", destinatario);
    }

    @Override
    public void enviarEmailRedefinicaoSenha(String destinatario, String nome, String token) {
        throw naoConfigurado("redefinicao de senha", destinatario);
    }

    private IllegalStateException naoConfigurado(String tipo, String destinatario) {
        log.error("Tentativa de enviar e-mail de {} para {} sem provedor configurado. "
                + "O fluxo deveria ter checado EmailSenderPort.habilitado() antes.", tipo, destinatario);
        return new IllegalStateException("Envio de e-mail nao esta configurado nesta instalacao.");
    }
}
