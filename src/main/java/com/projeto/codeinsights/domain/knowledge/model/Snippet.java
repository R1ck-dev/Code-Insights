package com.projeto.codeinsights.domain.knowledge.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.shared.exception.NegocioException;

/**
 * Snippet: trecho de codigo reutilizavel e categorizado, parte da biblioteca
 * pessoal de conhecimento do usuario. Pode opcionalmente estar vinculado a uma
 * {@code Resolucao} (de onde foi extraido) via {@code resolucaoId}.
 */
public class Snippet {

    private UUID id;
    private UUID autorId;
    private UUID resolucaoId;
    private String titulo;
    private String codigo;
    private String descricao;
    private String categoriaConceito;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    /** Construtor de criacao. */
    public Snippet(UUID id, UUID autorId, UUID resolucaoId, String titulo, String codigo,
            String descricao, String categoriaConceito) {
        if (autorId == null) {
            throw new NegocioException("O autor do snippet e obrigatorio.");
        }
        validarTitulo(titulo);
        validarCodigo(codigo);

        this.id = (id != null) ? id : UUID.randomUUID();
        this.autorId = autorId;
        this.resolucaoId = resolucaoId;
        this.titulo = titulo.trim();
        this.codigo = codigo;
        this.descricao = descricao;
        this.categoriaConceito = categoriaConceito;
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = this.dataCriacao;
    }

    /** Construtor de reconstituicao. */
    public Snippet(UUID id, UUID autorId, UUID resolucaoId, String titulo, String codigo,
            String descricao, String categoriaConceito, LocalDateTime dataCriacao,
            LocalDateTime dataAtualizacao) {
        this.id = id;
        this.autorId = autorId;
        this.resolucaoId = resolucaoId;
        this.titulo = titulo;
        this.codigo = codigo;
        this.descricao = descricao;
        this.categoriaConceito = categoriaConceito;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
    }

    public void atualizar(String titulo, String codigo, String descricao, String categoriaConceito) {
        validarTitulo(titulo);
        validarCodigo(codigo);
        this.titulo = titulo.trim();
        this.codigo = codigo;
        this.descricao = descricao;
        this.categoriaConceito = categoriaConceito;
        marcarAtualizacao();
    }

    private void validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new NegocioException("O titulo do snippet e obrigatorio.");
        }
    }

    private void validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new NegocioException("O codigo do snippet e obrigatorio.");
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

    public UUID getResolucaoId() {
        return resolucaoId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getCategoriaConceito() {
        return categoriaConceito;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }
}
