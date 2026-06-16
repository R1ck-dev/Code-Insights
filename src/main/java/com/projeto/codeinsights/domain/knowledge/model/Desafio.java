package com.projeto.codeinsights.domain.knowledge.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.DificuldadeDesafio;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

/**
 * Desafio: o enunciado de um problema de programacao no portfolio de um usuario.
 * Aggregate root. As solucoes ficam em {@code Resolucao} (1 Desafio : N Resolucoes),
 * referenciadas por id; a visibilidade publico/privado vive aqui.
 */
public class Desafio {

    private UUID id;
    private UUID autorId;
    private String titulo;
    private String descricao;
    private String origemPlataforma;
    private DificuldadeDesafio dificuldade;
    private boolean publico;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    /** Construtor de criacao. */
    public Desafio(UUID id, UUID autorId, String titulo, String descricao,
            String origemPlataforma, DificuldadeDesafio dificuldade) {
        if (autorId == null) {
            throw new NegocioException("O autor do desafio e obrigatorio.");
        }
        validarTitulo(titulo);

        this.id = (id != null) ? id : UUID.randomUUID();
        this.autorId = autorId;
        this.titulo = titulo.trim();
        this.descricao = descricao;
        this.origemPlataforma = origemPlataforma;
        this.dificuldade = dificuldade;
        this.publico = false;
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = this.dataCriacao;
    }

    /** Construtor de reconstituicao. */
    public Desafio(UUID id, UUID autorId, String titulo, String descricao, String origemPlataforma,
            DificuldadeDesafio dificuldade, boolean publico, LocalDateTime dataCriacao,
            LocalDateTime dataAtualizacao) {
        this.id = id;
        this.autorId = autorId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.origemPlataforma = origemPlataforma;
        this.dificuldade = dificuldade;
        this.publico = publico;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
    }

    public void atualizarDetalhes(String titulo, String descricao, String origemPlataforma,
            DificuldadeDesafio dificuldade) {
        validarTitulo(titulo);
        this.titulo = titulo.trim();
        this.descricao = descricao;
        this.origemPlataforma = origemPlataforma;
        this.dificuldade = dificuldade;
        marcarAtualizacao();
    }

    public void publicar() {
        this.publico = true;
        marcarAtualizacao();
    }

    public void despublicar() {
        this.publico = false;
        marcarAtualizacao();
    }

    private void validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new NegocioException("O titulo do desafio e obrigatorio.");
        }
    }

    private void marcarAtualizacao() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAutorId() {
        return autorId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getOrigemPlataforma() {
        return origemPlataforma;
    }

    public DificuldadeDesafio getDificuldade() {
        return dificuldade;
    }

    public boolean isPublico() {
        return publico;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }
}
