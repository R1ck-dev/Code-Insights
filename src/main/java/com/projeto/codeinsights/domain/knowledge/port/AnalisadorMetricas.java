package com.projeto.codeinsights.domain.knowledge.port;

import java.util.List;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;

/**
 * Porta de saida para o motor de analise estatica. O dominio define o contrato
 * ("dada uma resolucao, produza suas metricas") e a infraestrutura o implementa
 * com a analise da AST (via JavaParser), sem que o dominio conheca a biblioteca.
 * Retorna lista vazia quando a linguagem ainda nao e suportada ou o codigo nao
 * pode ser parseado.
 */
public interface AnalisadorMetricas {

    /**
     * Existe analisador para {@code linguagem}?
     * <p>
     * Separa "nao sei analisar esta linguagem" de "sei, mas o codigo nao parseou" — os dois
     * casos devolvem lista vazia em {@link #analisar(Resolucao)}, e a reanalise do corpus
     * precisa distingui-los: o primeiro e uma resolucao <i>pulada</i> (esperado, ha apenas
     * analisador de Java), o segundo e uma <i>falha</i> (o motor deveria ter dado conta).
     */
    boolean suporta(LinguagemProgramacao linguagem);

    List<ResultadoMetrica> analisar(Resolucao resolucao);
}
