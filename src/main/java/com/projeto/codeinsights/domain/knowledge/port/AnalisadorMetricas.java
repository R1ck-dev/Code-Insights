package com.projeto.codeinsights.domain.knowledge.port;

import java.util.List;
import java.util.Set;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;

/**
 * Porta de saida para o motor de analise estatica. O dominio define o contrato
 * ("dada uma resolucao, produza suas metricas") e a infraestrutura o implementa
 * por analise da AST, sem que o dominio conheca a biblioteca de parsing.
 * Retorna lista vazia quando a linguagem nao e suportada ou o codigo nao pode ser
 * parseado.
 */
public interface AnalisadorMetricas {

    /**
     * Quais metricas o motor sabe calcular para {@code linguagem} — <b>conjunto vazio</b> quando nao
     * ha analisador nenhum.
     * <p>
     * O suporte e <b>parcial por linguagem</b>, e nao um sim/nao: contar pontos de decisao
     * (ciclomatica) e uma contagem lexica, enquanto inferir Big-O exige um modelo de custo sobre a
     * AST inteira. Uma linguagem pode ter a primeira e nao a segunda — e foi por isso que este
     * metodo substituiu um booleano. Com o booleano, uma resolucao analisada em parte caía no balde
     * de <i>falha de analise</i> da tela de qualidade, que passaria a acusar defeito onde so falta
     * instrumento.
     */
    Set<TipoMetrica> metricasSuportadas(LinguagemProgramacao linguagem);

    /** Ha algum analisador para a linguagem, ainda que so para parte das metricas. */
    default boolean suporta(LinguagemProgramacao linguagem) {
        return !metricasSuportadas(linguagem).isEmpty();
    }

    /**
     * O motor produz esta metrica para esta linguagem? E a pergunta que os baldes de cobertura
     * precisam fazer: "sem dado" e escopo conhecido quando o motor nunca prometeu o numero, e
     * defeito quando prometeu e nao entregou.
     */
    default boolean produz(TipoMetrica tipo, LinguagemProgramacao linguagem) {
        return metricasSuportadas(linguagem).contains(tipo);
    }

    List<ResultadoMetrica> analisar(Resolucao resolucao);
}
