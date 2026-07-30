package com.projeto.codeinsights.infrastructure.persistence.identity.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.identity.port.EmailSenderPort;
import com.projeto.codeinsights.infrastructure.persistence.identity.adapter.MensagensDeEmail.Mensagem;

import lombok.RequiredArgsConstructor;

/**
 * Envia e-mail por SMTP. E o caminho do ambiente local, onde o MailHog escuta na 1026.
 * <p>
 * <b>Nao funciona no Render em plano gratuito</b>, que bloqueia a saida nas portas SMTP 25, 465 e
 * 587 — e bloqueia descartando os pacotes, sem recusar a conexao, o que pendura o cliente ate o
 * timeout. La o provedor tem de ser o {@link Smtp2goEmailAdapter}, que fala HTTPS na 443. A escolha
 * entre os dois e feita por {@code app.mail.provider}.
 */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
@RequiredArgsConstructor
public class JavaMailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final MensagensDeEmail mensagens;

    @Value("${app.mail.from}")
    private String remetente;

    @Override
    public void enviarEmailAtivacao(String destinatario, String nome, String token) {
        enviar(destinatario, mensagens.ativacaoDeConta(nome, token));
    }

    @Override
    public void enviarEmailRedefinicaoSenha(String destinatario, String nome, String token) {
        enviar(destinatario, mensagens.redefinicaoDeSenha(nome, token));
    }

    private void enviar(String destinatario, Mensagem mensagem) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(remetente);
        message.setTo(destinatario);
        message.setSubject(mensagem.assunto());
        message.setText(mensagem.corpo());

        mailSender.send(message);
    }
}
