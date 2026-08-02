package com.projeto.codeinsights.infrastructure.persistence.pesquisa.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.pesquisa.model.TermoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.port.TermoDeConsentimentoPort;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Le o TCLE vigente de {@code classpath:consentimento/termo-<versao>.md}.
 * <p>
 * Fica em {@code persistence/pesquisa/adapter} por ser o lugar que a arquitetura do projeto reserva a
 * adapters de porta do contexto, ainda que este nao toque no banco.
 * <p>
 * <b>Carrega no boot e falha o boot se o arquivo nao existir.</b> A alternativa — carregar sob
 * demanda — daria uma aplicacao que sobe saudavel e so quebra quando o primeiro participante abre a
 * tela do termo, que e o pior momento possivel para descobrir um erro de digitacao na versao. O
 * texto tambem nao muda em tempo de execucao, entao ler uma vez e o comportamento correto e nao uma
 * otimizacao.
 */
@Slf4j
@Component
public class TermoDeConsentimentoClasspathAdapter implements TermoDeConsentimentoPort {

    private static final String PADRAO_DO_ARQUIVO = "consentimento/termo-%s.md";

    private final String versao;
    private final boolean aprovadoPeloComite;
    private TermoDeConsentimento termo;

    public TermoDeConsentimentoClasspathAdapter(
            @Value("${app.pesquisa.consentimento.versao}") String versao,
            @Value("${app.pesquisa.consentimento.aprovado-pelo-comite}") boolean aprovadoPeloComite) {
        this.versao = versao;
        this.aprovadoPeloComite = aprovadoPeloComite;
    }

    @PostConstruct
    void carregar() {
        String caminho = PADRAO_DO_ARQUIVO.formatted(versao);
        ClassPathResource recurso = new ClassPathResource(caminho);

        if (!recurso.exists()) {
            throw new IllegalStateException(
                    ("Termo de consentimento nao encontrado em '%s'. A versao vigente e definida por "
                            + "app.pesquisa.consentimento.versao (env TERMO_CONSENTIMENTO_VERSAO) e "
                            + "precisa ter um arquivo correspondente no classpath.").formatted(caminho));
        }

        String texto = ler(recurso, caminho);
        this.termo = new TermoDeConsentimento(versao, tituloDe(texto), texto, aprovadoPeloComite);

        if (aprovadoPeloComite) {
            log.info("Termo de consentimento '{}' carregado, aprovado pelo Comite de Etica.", versao);
        } else {
            log.warn("Termo de consentimento '{}' carregado como RASCUNHO: aceites nao autorizam uso "
                    + "em pesquisa. Ligue app.pesquisa.consentimento.aprovado-pelo-comite quando "
                    + "houver parecer do CEP.", versao);
        }
    }

    @Override
    public TermoDeConsentimento vigente() {
        return termo;
    }

    private String ler(ClassPathResource recurso, String caminho) {
        try (InputStream entrada = recurso.getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o termo de consentimento '%s'.".formatted(caminho), e);
        }
    }

    /** Primeiro cabecalho de nivel 1 do markdown; sem ele, o proprio identificador da versao serve. */
    private String tituloDe(String texto) {
        return texto.lines()
                .filter(linha -> linha.startsWith("# "))
                .map(linha -> linha.substring(2).trim())
                .findFirst()
                .orElse("Termo de Consentimento " + versao);
    }
}
