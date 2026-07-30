package com.projeto.codeinsights.infrastructure.persistence.identity.adapter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Cobre o que falharia calado em producao: o formato do payload (nomes de campo errados sao
 * aceitos com 200 e o e-mail nunca sai) e a recusa que a API devolve dentro de uma resposta
 * de sucesso.
 */
class Smtp2goEmailAdapterTest {

    private static final String URL = "https://api.smtp2go.com/v3/email/send";
    private static final String CHAVE = "api-chave-de-teste";
    private static final String REMETENTE = "pesquisa@exemplo.com";

    private MockRestServiceServer servidor;
    private Smtp2goEmailAdapter adapter;

    @BeforeEach
    void configurar() {
        MensagensDeEmail mensagens = new MensagensDeEmail();
        ReflectionTestUtils.setField(mensagens, "webBaseUrl", "https://app.exemplo.com");

        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();

        adapter = new Smtp2goEmailAdapter(mensagens, builder.build());
        ReflectionTestUtils.setField(adapter, "url", URL);
        ReflectionTestUtils.setField(adapter, "apiKey", CHAVE);
        ReflectionTestUtils.setField(adapter, "remetente", REMETENTE);
    }

    private String respostaCom(int sucessos, int falhas) {
        return """
                {"data":{"succeeded":%d,"failed":%d,"failures":[]}}
                """.formatted(sucessos, falhas);
    }

    @Test
    void enviaAtivacaoNoFormatoQueApiEspera() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Smtp2go-Api-Key", CHAVE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sender").value(REMETENTE))
                .andExpect(jsonPath("$.to[0]").value("aluno@exemplo.com"))
                .andExpect(jsonPath("$.subject").value("Ative sua conta no CodeInsights"))
                // O link e o unico conteudo que precisa estar exato: errado, o aluno nao ativa.
                .andExpect(jsonPath("$.text_body")
                        .value(org.hamcrest.Matchers.containsString(
                                "https://app.exemplo.com/ativar?token=abc-123")))
                .andRespond(withSuccess(respostaCom(1, 0), MediaType.APPLICATION_JSON));

        adapter.enviarEmailAtivacao("aluno@exemplo.com", "Aluno", "abc-123");

        servidor.verify();
    }

    @Test
    void enviaRedefinicaoComOLinkDeDefinirSenha() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.subject").value("Redefinicao de senha no CodeInsights"))
                .andExpect(jsonPath("$.text_body")
                        .value(org.hamcrest.Matchers.containsString(
                                "https://app.exemplo.com/definir-senha?token=xyz-789")))
                .andRespond(withSuccess(respostaCom(1, 0), MediaType.APPLICATION_JSON));

        assertThatCode(() -> adapter.enviarEmailRedefinicaoSenha("aluno@exemplo.com", "Aluno", "xyz-789"))
                .doesNotThrowAnyException();

        servidor.verify();
    }

    /**
     * O caso perigoso: remetente nao verificado no SMTP2GO devolve HTTP 200 com succeeded=0.
     * Sem a checagem no adapter, o cadastro daria certo e o e-mail nunca chegaria.
     */
    @Test
    void falhaQuandoApiRespondeSucessoMasRecusaOEnvio() {
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess(respostaCom(0, 1), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.enviarEmailAtivacao("aluno@exemplo.com", "Aluno", "abc-123"))
                .isInstanceOf(IllegalStateException.class);
    }
}
