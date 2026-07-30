package com.projeto.codeinsights.infrastructure.config.mail;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP usado para falar com a API do SMTP2GO.
 * <p>
 * Fica separado do adapter porque configurar transporte e responsabilidade de infraestrutura, nao
 * de quem monta a mensagem — e porque um adapter que constroi o proprio cliente nao tem como ser
 * testado contra um servidor simulado.
 * <p>
 * <b>Os timeouts sao o ponto principal deste bean.</b> Sem eles o cliente espera para sempre por
 * uma resposta que pode nunca vir, e foi exatamente assim que uma indisponibilidade de e-mail
 * virou cadastro travado em producao.
 */
@Configuration
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp2go")
public class Smtp2goClientConfig {

    private static final Duration TIMEOUT_CONEXAO = Duration.ofSeconds(5);
    private static final Duration TIMEOUT_LEITURA = Duration.ofSeconds(15);

    @Bean
    public RestClient smtp2goRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(TIMEOUT_CONEXAO);
        fabrica.setReadTimeout(TIMEOUT_LEITURA);

        return builder.requestFactory(fabrica).build();
    }
}
