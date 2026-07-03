package com.projeto.codeinsights.infrastructure.metrica;

/**
 * Resultado bruto de um analisador de metrica sobre a AST, antes de virar o
 * agregado de dominio {@code ResultadoMetrica}. {@code valor} e a magnitude
 * numerica; {@code rotulo} a forma de exibicao; {@code detalhe} o raciocinio.
 */
public record MetricaCalculada(int valor, String rotulo, String detalhe) {
}
