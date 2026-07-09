package com.projeto.codeinsights.infrastructure.persistence.knowledge.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;

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
@Table(name = "resultados_metrica")
@Getter
@Setter
public class ResultadoMetricaJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolucao_id", nullable = false)
    private ResolucaoJpaEntity resolucao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoMetrica tipo;

    @Column(nullable = false)
    private int valor;

    @Column(nullable = false, length = 50)
    private String rotulo;

    @Column(columnDefinition = "TEXT")
    private String detalhe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NivelConfianca confianca;

    @Column(name = "analisado_em", nullable = false, updatable = false)
    private OffsetDateTime analisadoEm;
}
