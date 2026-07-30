package com.projeto.codeinsights.infrastructure.persistence.identity.adapter;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.projeto.codeinsights.domain.identity.port.EmailSenderPort;
import com.projeto.codeinsights.infrastructure.persistence.identity.adapter.MensagensDeEmail.Mensagem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Envia e-mail pela API HTTP do SMTP2GO, e nao por SMTP.
 * <p>
 * <b>Por que HTTP.</b> O plano gratuito do Render bloqueia trafego de saida nas portas SMTP 25,
 * 465 e 587. O bloqueio descarta os pacotes em silencio em vez de recusar a conexao, entao o
 * cliente SMTP fica esperando uma resposta que nunca chega — foi o que travava o cadastro. Esta
 * API roda sobre HTTPS na 443, que nao e bloqueada.
 * <p>
 * <b>Timeouts sao obrigatorios aqui.</b> Foi justamente a ausencia deles que transformou uma
 * indisponibilidade de e-mail em requisicao pendurada para sempre. Com eles, o pior caso vira um
 * erro em segundos.
 */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp2go")
@RequiredArgsConstructor
@Slf4j
public class Smtp2goEmailAdapter implements EmailSenderPort {

    private final MensagensDeEmail mensagens;

    /** Ja vem com os timeouts aplicados por {@code Smtp2goClientConfig}. */
    private final RestClient client;

    @Value("${app.mail.smtp2go.url}")
    private String url;

    @Value("${app.mail.smtp2go.api-key}")
    private String apiKey;

    @Value("${app.mail.from}")
    private String remetente;

    @Override
    public boolean habilitado() {
        return true;
    }

    @Override
    public void enviarEmailAtivacao(String destinatario, String nome, String token) {
        enviar(destinatario, mensagens.ativacaoDeConta(nome, token));
    }

    @Override
    public void enviarEmailRedefinicaoSenha(String destinatario, String nome, String token) {
        enviar(destinatario, mensagens.redefinicaoDeSenha(nome, token));
    }

    private void enviar(String destinatario, Mensagem mensagem) {
        Requisicao corpo = new Requisicao(remetente, List.of(destinatario), mensagem.assunto(), mensagem.corpo());

        Resposta resposta = client.post()
                .uri(url)
                .header("X-Smtp2go-Api-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .body(Resposta.class);

        // A API responde 200 mesmo quando recusa o envio (remetente nao verificado, destinatario
        // invalido): o veredito real esta em data.succeeded. Sem esta checagem, um remetente nao
        // verificado no SMTP2GO passaria por sucesso e o e-mail simplesmente nunca chegaria.
        if (resposta == null || resposta.data() == null || resposta.data().succeeded() < 1) {
            List<String> falhas = (resposta != null && resposta.data() != null) ? resposta.data().failures() : null;
            log.error("SMTP2GO recusou o envio para {}. Falhas: {}", destinatario, falhas);
            throw new IllegalStateException("Nao foi possivel enviar o e-mail no momento.");
        }
    }

    private record Requisicao(String sender, List<String> to, String subject, String text_body) {
    }

    private record Resposta(Dados data) {
        private record Dados(int succeeded, int failed, List<String> failures) {
        }
    }
}
