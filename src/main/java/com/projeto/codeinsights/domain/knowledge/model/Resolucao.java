package com.projeto.codeinsights.domain.knowledge.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.shared.exception.NegocioException;

/**
 * Resolucao: uma solucao submetida por um usuario para um {@code Desafio}.
 * Guarda o codigo-fonte e os campos de metrica de aprendizado.
 * <p>
 * As metricas (indice de autonomia IA 1-5, complexidade de tempo/espaco e
 * ciclomatica) sao o nucleo da pesquisa, mas neste ciclo NAO sao calculadas:
 * nascem nulas e so serao preenchidas quando a extracao/analise for adicionada.
 * O metodo {@link #registrarMetricas} ja existe como ponto de extensao.
 */
public class Resolucao {

    private UUID id;
    private UUID desafioId;
    private UUID autorId;
    private String linguagem;
    private String codigoFonte;
    private Integer indiceAutonomiaIa;
    private String complexidadeTempo;
    private String complexidadeEspaco;
    private Integer complexidadeCiclomatica;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    /** Construtor de criacao: metricas comecam vazias. */
    public Resolucao(UUID id, UUID desafioId, UUID autorId, String linguagem, String codigoFonte) {
        if (desafioId == null) {
            throw new NegocioException("O desafio da resolucao e obrigatorio.");
        }
        if (autorId == null) {
            throw new NegocioException("O autor da resolucao e obrigatorio.");
        }
        validarCodigoFonte(codigoFonte);

        this.id = (id != null) ? id : UUID.randomUUID();
        this.desafioId = desafioId;
        this.autorId = autorId;
        this.linguagem = linguagem;
        this.codigoFonte = codigoFonte;
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = this.dataCriacao;
    }

    /** Construtor de reconstituicao. */
    public Resolucao(UUID id, UUID desafioId, UUID autorId, String linguagem, String codigoFonte,
            Integer indiceAutonomiaIa, String complexidadeTempo, String complexidadeEspaco,
            Integer complexidadeCiclomatica, LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
        this.id = id;
        this.desafioId = desafioId;
        this.autorId = autorId;
        this.linguagem = linguagem;
        this.codigoFonte = codigoFonte;
        this.indiceAutonomiaIa = indiceAutonomiaIa;
        this.complexidadeTempo = complexidadeTempo;
        this.complexidadeEspaco = complexidadeEspaco;
        this.complexidadeCiclomatica = complexidadeCiclomatica;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
    }

    public void atualizarCodigo(String linguagem, String codigoFonte) {
        validarCodigoFonte(codigoFonte);
        this.linguagem = linguagem;
        this.codigoFonte = codigoFonte;
        marcarAtualizacao();
    }

    /**
     * Ponto de extensao para as metricas de aprendizado (a ser usado quando a
     * analise for implementada). O indice de autonomia IA deve estar entre 1 e 5.
     */
    public void registrarMetricas(Integer indiceAutonomiaIa, String complexidadeTempo,
            String complexidadeEspaco, Integer complexidadeCiclomatica) {
        if (indiceAutonomiaIa != null && (indiceAutonomiaIa < 1 || indiceAutonomiaIa > 5)) {
            throw new NegocioException("O indice de autonomia IA deve estar entre 1 e 5.");
        }
        this.indiceAutonomiaIa = indiceAutonomiaIa;
        this.complexidadeTempo = complexidadeTempo;
        this.complexidadeEspaco = complexidadeEspaco;
        this.complexidadeCiclomatica = complexidadeCiclomatica;
        marcarAtualizacao();
    }

    private void validarCodigoFonte(String codigoFonte) {
        if (codigoFonte == null || codigoFonte.isBlank()) {
            throw new NegocioException("O codigo-fonte da resolucao e obrigatorio.");
        }
    }

    private void marcarAtualizacao() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDesafioId() {
        return desafioId;
    }

    public UUID getAutorId() {
        return autorId;
    }

    public String getLinguagem() {
        return linguagem;
    }

    public String getCodigoFonte() {
        return codigoFonte;
    }

    public Integer getIndiceAutonomiaIa() {
        return indiceAutonomiaIa;
    }

    public String getComplexidadeTempo() {
        return complexidadeTempo;
    }

    public String getComplexidadeEspaco() {
        return complexidadeEspaco;
    }

    public Integer getComplexidadeCiclomatica() {
        return complexidadeCiclomatica;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }
}
