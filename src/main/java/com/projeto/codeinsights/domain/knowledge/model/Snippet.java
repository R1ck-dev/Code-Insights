package com.projeto.codeinsights.domain.knowledge.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.CategoriaConceito;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

/**
 * Snippet: trecho de codigo reutilizavel e categorizado, parte da biblioteca
 * pessoal de conhecimento do usuario. Referencia o autor por id e classifica o
 * conceito via enum {@code CategoriaConceito}.
 */
public class Snippet {

    private UUID id;
    private UUID autorId;
    private String codigo;
    private String descricao;
    private CategoriaConceito categoria;
    private OffsetDateTime criadoEm;

    /** Construtor de criacao. */
    public Snippet(UUID id, UUID autorId, String codigo, String descricao, CategoriaConceito categoria) {
        if (autorId == null) {
            throw new NegocioException("O autor do snippet e obrigatorio.");
        }
        validarCodigo(codigo);
        validarCategoria(categoria);

        this.id = (id != null) ? id : UUID.randomUUID();
        this.autorId = autorId;
        this.codigo = codigo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.criadoEm = OffsetDateTime.now();
    }

    /** Construtor de reconstituicao. */
    public Snippet(UUID id, UUID autorId, String codigo, String descricao, CategoriaConceito categoria,
            OffsetDateTime criadoEm) {
        this.id = id;
        this.autorId = autorId;
        this.codigo = codigo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.criadoEm = criadoEm;
    }

    public void atualizarConteudo(String codigo, String descricao) {
        validarCodigo(codigo);
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public void recategorizar(CategoriaConceito nova) {
        validarCategoria(nova);
        this.categoria = nova;
    }

    public boolean pertenceA(UUID usuarioId) {
        return this.autorId.equals(usuarioId);
    }

    private void validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new NegocioException("O codigo do snippet e obrigatorio.");
        }
    }

    private void validarCategoria(CategoriaConceito categoria) {
        if (categoria == null) {
            throw new NegocioException("A categoria do snippet e obrigatoria.");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getAutorId() {
        return autorId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public CategoriaConceito getCategoria() {
        return categoria;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
