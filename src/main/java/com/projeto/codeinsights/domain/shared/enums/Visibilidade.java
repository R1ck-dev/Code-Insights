package com.projeto.codeinsights.domain.shared.enums;

/**
 * Visibilidade de um recurso (perfil, desafio, resolucao): publico ou privado.
 * Tipo transversal do dominio, compartilhado pelos contextos {@code identity} e
 * {@code knowledge}. Substitui os antigos campos boolean {@code isProfilePublic}/
 * {@code isPublic}.
 */
public enum Visibilidade {
    PUBLICO,
    PRIVADO
}
