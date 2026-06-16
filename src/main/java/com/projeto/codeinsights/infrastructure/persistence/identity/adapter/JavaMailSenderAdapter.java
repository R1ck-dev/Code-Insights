package com.projeto.codeinsights.infrastructure.persistence.identity.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.identity.port.EmailSenderPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JavaMailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;

    @Value("${app.web.base-url}")
    private String webBaseUrl;

    @Value("${app.mail.from}")
    private String remetente;

    @Override
    public void enviarEmailAtivacao(String destinatario, String nome, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(remetente);
        message.setTo(destinatario);
        message.setSubject("Ative sua conta no CodeInsights");

        String urlAtivacao = webBaseUrl + "/ativar?token=" + token;

        message.setText("Ola, " + nome + "!\n\n"
                + "Bem-vindo ao CodeInsights. Para comecar a usar a plataforma, confirme seu e-mail "
                + "clicando no link abaixo:\n"
                + urlAtivacao + "\n\n"
                + "O link e valido por 24 horas.");

        mailSender.send(message);
    }
}
