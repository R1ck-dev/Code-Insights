package com.projeto.codeinsights.domain.knowledge.model;

/**
 * Value object: um ponto da serie temporal de submissoes de um autor, no inicio
 * do periodo agrupado (dia/semana/mes). {@code dia} carrega o dia do inicio do
 * periodo (1 quando a granularidade e mensal). {@code mediaComplexidade} e a
 * media da ordem da classe de Big O (tempo) das resolucoes analisadas do periodo
 * (null quando nenhuma tem metrica valida).
 */
public record PontoEvolucaoMensal(int ano, int mes, int dia, Double mediaAutonomia,
        long totalResolucoes, Double mediaComplexidade) {
}
