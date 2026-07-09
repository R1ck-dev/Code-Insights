package com.projeto.codeinsights.infrastructure.metrica;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;

/**
 * Resultado bruto de um analisador de metrica sobre a AST, antes de virar o
 * agregado de dominio {@code ResultadoMetrica}. {@code valor} e a magnitude
 * numerica; {@code rotulo} a forma de exibicao; {@code detalhe} o raciocinio;
 * {@code confianca} o quanto o motor confia no que calculou.
 */
public record MetricaCalculada(int valor, String rotulo, String detalhe, NivelConfianca confianca) {
}
