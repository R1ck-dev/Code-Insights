package com.projeto.codeinsights.infrastructure.persistence.knowledge.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.projeto.codeinsights.infrastructure.persistence.identity.entity.UsuarioJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "snippets")
@Getter
@Setter
public class SnippetJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private UsuarioJpaEntity autor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolucao_id")
    private ResolucaoJpaEntity resolucao;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String codigo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "categoria_conceito", length = 100)
    private String categoriaConceito;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;
}
