package com.projeto.codeinsights.domain.identity.enums;

/**
 * Papel do usuario na plataforma. Os atores do CodeInsights diferem apenas em
 * autorizacao (nao em atributos), por isso o papel e um enum e nao uma hierarquia
 * de subclasses.
 */
public enum Role {
    ALUNO,
    PESQUISADOR,
    ADMIN
}
