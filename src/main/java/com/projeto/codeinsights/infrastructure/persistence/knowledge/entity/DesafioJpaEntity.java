package com.projeto.codeinsights.infrastructure.persistence.knowledge.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

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
    private String enunciado;

    @Column(name = "plataforma_origem", length = 100)
    private String plataformaOrigem;

    @Column(name = "identificador_externo", length = 100)
    private String identificadorExterno;

    @Column(name = "url_externa", length = 500)
    private String urlExterna;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibilidade visibilidade;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;
}
