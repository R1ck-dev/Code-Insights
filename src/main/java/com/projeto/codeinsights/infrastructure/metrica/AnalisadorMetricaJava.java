package com.projeto.codeinsights.infrastructure.metrica;

import com.github.javaparser.ast.CompilationUnit;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;

/**
 * SPI de uma metrica calculada sobre a AST Java. Cada metrica e um bean isolado:
 * adicionar uma nova metrica = adicionar uma nova implementacao, sem tocar nas
 * demais nem no orquestrador (que injeta todas as implementacoes).
 */
public interface AnalisadorMetricaJava {

    TipoMetrica tipo();

    MetricaCalculada analisar(CompilationUnit unidade);
}
