package com.projeto.codeinsights.domain.knowledge.port;

import java.util.List;

import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;

/**
 * Porta de saida para o motor de analise estatica. O dominio define o contrato
 * ("dada uma resolucao, produza suas metricas") e a infraestrutura o implementa
 * com a analise da AST (via JavaParser), sem que o dominio conheca a biblioteca.
 * Retorna lista vazia quando a linguagem ainda nao e suportada ou o codigo nao
 * pode ser parseado.
 */
public interface AnalisadorMetricas {
    List<ResultadoMetrica> analisar(Resolucao resolucao);
}
