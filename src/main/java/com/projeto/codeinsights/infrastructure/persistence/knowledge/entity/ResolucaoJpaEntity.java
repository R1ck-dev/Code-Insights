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

    @Column(length = 50)
    private String linguagem;

    @Column(name = "codigo_fonte", nullable = false, columnDefinition = "TEXT")
    private String codigoFonte;

    @Column(name = "indice_autonomia_ia")
    private Integer indiceAutonomiaIa;

    @Column(name = "complexidade_tempo", length = 50)
    private String complexidadeTempo;

    @Column(name = "complexidade_espaco", length = 50)
    private String complexidadeEspaco;

    @Column(name = "complexidade_ciclomatica")
    private Integer complexidadeCiclomatica;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;
}
