package com.projeto.codeinsights.domain.knowledge.model;

/**
 * Value object: um ponto da serie temporal de submissoes de um autor (por mes).
 * {@code mediaComplexidade} e a media da ordem da classe de Big O (tempo) das
 * resolucoes analisadas do mes (null quando nenhuma tem metrica valida).
 */
public record PontoEvolucaoMensal(int ano, int mes, Double mediaAutonomia, long totalResolucoes,
        Double mediaComplexidade) {
}
