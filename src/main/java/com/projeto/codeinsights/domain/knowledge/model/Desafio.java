package com.projeto.codeinsights.domain.knowledge.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.shared.enums.Visibilidade;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

/**
 * Desafio: um exercicio resolvido pelo usuario num Online Judge externo
 * (NepsAcademy, LeetCode, Codeforces...), registrado no seu portfolio pessoal.
 * Aggregate root. As solucoes ficam em {@code Resolucao} (1 Desafio : N Resolucoes),
 * referenciadas por id; a visibilidade publico/privado vive aqui.
 */
public class Desafio {

    private UUID id;
    private UUID autorId;
    private String titulo;
    private String enunciado;
    private String plataformaOrigem;
    private String identificadorExterno;
    private String urlExterna;
    private Visibilidade visibilidade;
    private OffsetDateTime criadoEm;
    private OffsetDateTime atualizadoEm;

    /** Construtor de criacao. */
    public Desafio(UUID id, UUID autorId, String titulo, String enunciado, String plataformaOrigem,
            String identificadorExterno, String urlExterna) {
        if (autorId == null) {
            throw new NegocioException("O autor do desafio e obrigatorio.");
        }
        validarTitulo(titulo);

        this.id = (id != null) ? id : UUID.randomUUID();
        this.autorId = autorId;
        this.titulo = titulo.trim();
        this.enunciado = enunciado;
        this.plataformaOrigem = plataformaOrigem;
        this.identificadorExterno = identificadorExterno;
        this.urlExterna = urlExterna;
        this.visibilidade = Visibilidade.PRIVADO;
        this.criadoEm = OffsetDateTime.now();
        this.atualizadoEm = this.criadoEm;
    }

    /** Construtor de reconstituicao. */
    public Desafio(UUID id, UUID autorId, String titulo, String enunciado, String plataformaOrigem,
            String identificadorExterno, String urlExterna, Visibilidade visibilidade,
            OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {
        this.id = id;
        this.autorId = autorId;
        this.titulo = titulo;
        this.enunciado = enunciado;
        this.plataformaOrigem = plataformaOrigem;
        this.identificadorExterno = identificadorExterno;
        this.urlExterna = urlExterna;
        this.visibilidade = visibilidade;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public void atualizarDetalhes(String titulo, String enunciado, String plataformaOrigem,
            String identificadorExterno, String urlExterna) {
        validarTitulo(titulo);
        this.titulo = titulo.trim();
        this.enunciado = enunciado;
        this.plataformaOrigem = plataformaOrigem;
        this.identificadorExterno = identificadorExterno;
        this.urlExterna = urlExterna;
        marcarAtualizacao();
    }

    public void atualizarEnunciado(String texto) {
        this.enunciado = texto;
        marcarAtualizacao();
    }

    public void publicar() {
        this.visibilidade = Visibilidade.PUBLICO;
        marcarAtualizacao();
    }

    public void ocultar() {
        this.visibilidade = Visibilidade.PRIVADO;
        marcarAtualizacao();
    }

    public boolean pertenceA(UUID usuarioId) {
        return this.autorId.equals(usuarioId);
    }

    public boolean ehPublico() {
        return this.visibilidade == Visibilidade.PUBLICO;
    }

    private void validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new NegocioException("O titulo do desafio e obrigatorio.");
        }
    }

    private void marcarAtualizacao() {
        this.atualizadoEm = OffsetDateTime.now();
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

    public String getEnunciado() {
        return enunciado;
    }

    public String getPlataformaOrigem() {
        return plataformaOrigem;
    }

    public String getIdentificadorExterno() {
        return identificadorExterno;
    }

    public String getUrlExterna() {
        return urlExterna;
    }

    public Visibilidade getVisibilidade() {
        return visibilidade;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
