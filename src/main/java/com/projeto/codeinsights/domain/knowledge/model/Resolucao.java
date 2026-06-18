package com.projeto.codeinsights.domain.knowledge.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

/**
 * Resolucao: uma solucao submetida por um usuario para um {@code Desafio}.
 * Guarda o codigo-fonte, a linguagem e o Indice de Autonomia IA (1-5,
 * autodeclarado pelo aluno no momento da submissao).
 * <p>
 * As metricas estaticas (Big O, ciclomatica, espaco) sao calculadas por analise
 * da AST e materializadas como {@code ResultadoMetrica} (ainda nao implementado);
 * a flag {@link #analisada} marca quando a analise ja rodou.
 */
public class Resolucao {

    /** Limites do Indice de Autonomia IA (autodeclarado). */
    private static final int AUTONOMIA_MINIMA = 1;
    private static final int AUTONOMIA_MAXIMA = 5;

    private UUID id;
    private UUID autorId;
    private UUID desafioId;
    private String codigoFonte;
    private LinguagemProgramacao linguagem;
    private int indiceAutonomiaIA;
    private String descricaoApoioIA;
    private Visibilidade visibilidade;
    private boolean analisada;
    private OffsetDateTime submetidaEm;

    /** Construtor de criacao: captura o codigo e o indice autodeclarado. */
    public Resolucao(UUID id, UUID autorId, UUID desafioId, String codigoFonte,
            LinguagemProgramacao linguagem, int indiceAutonomiaIA, String descricaoApoioIA) {
        if (autorId == null) {
            throw new NegocioException("O autor da resolucao e obrigatorio.");
        }
        if (desafioId == null) {
            throw new NegocioException("O desafio da resolucao e obrigatorio.");
        }
        if (linguagem == null) {
            throw new NegocioException("A linguagem da resolucao e obrigatoria.");
        }
        validarCodigoFonte(codigoFonte);
        validarAutonomia(indiceAutonomiaIA);

        this.id = (id != null) ? id : UUID.randomUUID();
        this.autorId = autorId;
        this.desafioId = desafioId;
        this.codigoFonte = codigoFonte;
        this.linguagem = linguagem;
        this.indiceAutonomiaIA = indiceAutonomiaIA;
        this.descricaoApoioIA = descricaoApoioIA;
        this.visibilidade = Visibilidade.PRIVADO;
        this.analisada = false;
        this.submetidaEm = OffsetDateTime.now();
    }

    /** Construtor de reconstituicao. */
    public Resolucao(UUID id, UUID autorId, UUID desafioId, String codigoFonte,
            LinguagemProgramacao linguagem, int indiceAutonomiaIA, String descricaoApoioIA,
            Visibilidade visibilidade, boolean analisada, OffsetDateTime submetidaEm) {
        this.id = id;
        this.autorId = autorId;
        this.desafioId = desafioId;
        this.codigoFonte = codigoFonte;
        this.linguagem = linguagem;
        this.indiceAutonomiaIA = indiceAutonomiaIA;
        this.descricaoApoioIA = descricaoApoioIA;
        this.visibilidade = visibilidade;
        this.analisada = analisada;
        this.submetidaEm = submetidaEm;
    }

    public void atualizarCodigo(String codigoFonte, LinguagemProgramacao linguagem) {
        validarCodigoFonte(codigoFonte);
        if (linguagem == null) {
            throw new NegocioException("A linguagem da resolucao e obrigatoria.");
        }
        this.codigoFonte = codigoFonte;
        this.linguagem = linguagem;
        this.analisada = false;
    }

    public void registrarAutonomia(int valor) {
        validarAutonomia(valor);
        this.indiceAutonomiaIA = valor;
    }

    public void marcarComoAnalisada() {
        this.analisada = true;
    }

    public void publicar() {
        this.visibilidade = Visibilidade.PUBLICO;
    }

    public boolean pertenceA(UUID usuarioId) {
        return this.autorId.equals(usuarioId);
    }

    public boolean ehPublica() {
        return this.visibilidade == Visibilidade.PUBLICO;
    }

    private void validarCodigoFonte(String codigoFonte) {
        if (codigoFonte == null || codigoFonte.isBlank()) {
            throw new NegocioException("O codigo-fonte da resolucao e obrigatorio.");
        }
    }

    private void validarAutonomia(int valor) {
        if (valor < AUTONOMIA_MINIMA || valor > AUTONOMIA_MAXIMA) {
            throw new NegocioException("O Indice de Autonomia IA deve estar entre 1 e 5.");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getAutorId() {
        return autorId;
    }

    public UUID getDesafioId() {
        return desafioId;
    }

    public String getCodigoFonte() {
        return codigoFonte;
    }

    public LinguagemProgramacao getLinguagem() {
        return linguagem;
    }

    public int getIndiceAutonomiaIA() {
        return indiceAutonomiaIA;
    }

    public String getDescricaoApoioIA() {
        return descricaoApoioIA;
    }

    public Visibilidade getVisibilidade() {
        return visibilidade;
    }

    public boolean isAnalisada() {
        return analisada;
    }

    public OffsetDateTime getSubmetidaEm() {
        return submetidaEm;
    }
}
