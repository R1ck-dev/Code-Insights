package com.projeto.codeinsights.infrastructure.persistence.knowledge.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;
import com.projeto.codeinsights.infrastructure.persistence.identity.entity.UsuarioJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "resolucoes")
@Getter
@Setter
public class ResolucaoJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "desafio_id", nullable = false)
    private DesafioJpaEntity desafio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private UsuarioJpaEntity autor;

    @Column(name = "codigo_fonte", nullable = false, columnDefinition = "TEXT")
    private String codigoFonte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LinguagemProgramacao linguagem;

    @Column(name = "indice_autonomia_ia", nullable = false)
    private int indiceAutonomiaIA;

    @Column(name = "descricao_apoio_ia", columnDefinition = "TEXT")
    private String descricaoApoioIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibilidade visibilidade;

    @Column(nullable = false)
    private boolean analisada;

    @Column(name = "submetida_em", nullable = false, updatable = false)
    private OffsetDateTime submetidaEm;
}
