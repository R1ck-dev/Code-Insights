package com.projeto.codeinsights.infrastructure.persistence.knowledge.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.DificuldadeDesafio;
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
@Table(name = "desafios")
@Getter
@Setter
public class DesafioJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private UsuarioJpaEntity autor;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "origem_plataforma", length = 100)
    private String origemPlataforma;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private DificuldadeDesafio dificuldade;

    @Column(nullable = false)
    private boolean publico;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;
}
