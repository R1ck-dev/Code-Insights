package com.projeto.codeinsights.infrastructure.persistence.identity.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Texto e assunto dos e-mails transacionais, num lugar so.
 * <p>
 * Existe porque ha mais de um adapter de {@code EmailSenderPort} (SMTP e API HTTP) e o canal de
 * entrega nao deveria decidir o que a mensagem diz. Sem isto, o link de ativacao viveria duplicado
 * em dois arquivos e um deles ficaria para tras na primeira alteracao — uma divergencia que so
 * apareceria na caixa de entrada do aluno.
 */
@Component
public class MensagensDeEmail {

    @Value("${app.web.base-url}")
    private String webBaseUrl;

    /** Assunto e corpo prontos de um e-mail transacional. */
    public record Mensagem(String assunto, String corpo) {
    }

    public Mensagem ativacaoDeConta(String nome, String token) {
        String url = webBaseUrl + "/ativar?token=" + token;

        return new Mensagem("Ative sua conta no CodeInsights",
                "Ola, " + nome + "!\n\n"
                        + "Bem-vindo ao CodeInsights. Para comecar a usar a plataforma, confirme seu e-mail "
                        + "clicando no link abaixo:\n"
                        + url + "\n\n"
                        + "O link e valido por 24 horas.");
    }

    public Mensagem redefinicaoDeSenha(String nome, String token) {
        String url = webBaseUrl + "/definir-senha?token=" + token;

        return new Mensagem("Redefinicao de senha no CodeInsights",
                "Ola, " + nome + "!\n\n"
                        + "Recebemos um pedido para redefinir a sua senha. Clique no link abaixo para "
                        + "escolher uma nova senha:\n"
                        + url + "\n\n"
                        + "Se voce nao solicitou, ignore este e-mail. O link e valido por 24 horas.");
    }
}
