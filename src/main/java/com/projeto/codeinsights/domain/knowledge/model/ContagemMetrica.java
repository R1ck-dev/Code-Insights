package com.projeto.codeinsights.domain.knowledge.model;

/** Value object: quantas resolucoes do autor caem em um rotulo de complexidade. */
public record ContagemMetrica(String rotulo, int ordem, long total) {
}
