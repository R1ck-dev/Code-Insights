package com.projeto.codeinsights.infrastructure.persistence.pesquisa.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.pesquisa.enums.DecisaoDeConsentimento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * O participante e guardado como {@code UUID} cru, sem {@code @ManyToOne} para
 * {@code UsuarioJpaEntity}. E deliberado: o contexto {@code pesquisa} nao deve poder navegar ate o
 * modelo de {@code identity} — a integridade e garantida pela FK no banco (V8), nao por um grafo de
 * objetos que abriria caminho para carregar dado de usuario onde ele nao deveria aparecer.
 * <p>
 * Todas as colunas sao {@code updatable = false}: a tabela e append-only, e isso e a defesa contra
 * alguem "corrigir" uma decisao com um {@code save()} distraido em vez de registrar uma nova.
 */
@Entity
@Table(name = "consentimentos_pesquisa")
@Getter
@Setter
public class ConsentimentoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "participante_id", nullable = false, updatable = false)
    private UUID participanteId;

    @Column(name = "versao_termo", nullable = false, length = 20, updatable = false)
    private String versaoDoTermo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private DecisaoDeConsentimento decisao;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    private OffsetDateTime registradoEm;
}
