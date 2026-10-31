package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;

/**
 * O que uma condicao garante a quem executa um ramo dela. Um ramo que executa quando a condicao e
 * <b>verdadeira</b> (o {@code then} de um {@code if} ou de um ternario) so enxerga como garantia os
 * conjuntos {@code &&}; um ramo que executa quando ela e <b>falsa</b> (o {@code else} de um ternario, o
 * que vem depois de um {@code if (...) continue;}) so enxerga os termos {@code ||}, todos falsos. Um
 * {@code ||} no primeiro caso, ou um {@code &&} no segundo, nao garante nenhum dos lados.
 * <p>
 * Nao se verifica a polaridade da consulta (se ela testa "ja marcado" ou "ainda livre"): cabe ao
 * programa que protege uma chamada ter escrito a consulta do lado certo.
 */
final class CondicaoDeRamo {

    private static final Set<BinaryExpr.Operator> RELACIONAIS = EnumSet.of(
            BinaryExpr.Operator.LESS, BinaryExpr.Operator.LESS_EQUALS,
            BinaryExpr.Operator.GREATER, BinaryExpr.Operator.GREATER_EQUALS);

    private CondicaoDeRamo() {
    }

    /** Os conjuntos {@code &&}: verdadeiros sempre que o ramo "then" executa. */
    static List<Expression> verdadeiras(Expression condicao) {
        Expression nua = semParenteses(condicao);
        if (nua instanceof BinaryExpr binaria && binaria.getOperator() == BinaryExpr.Operator.AND) {
            return unir(verdadeiras(binaria.getLeft()), verdadeiras(binaria.getRight()));
        }
        return garanteQuandoVerdadeira(nua) ? List.of(nua) : List.of();
    }

    /** Os termos {@code ||}: falsos sempre que o ramo "else" (ou o que segue um {@code continue}) executa. */
    static List<Expression> falsas(Expression condicao) {
        Expression nua = semParenteses(condicao);
        if (nua instanceof BinaryExpr binaria && binaria.getOperator() == BinaryExpr.Operator.OR) {
            return unir(falsas(binaria.getLeft()), falsas(binaria.getRight()));
        }
        return garanteQuandoFalsa(nua) ? List.of(nua) : List.of();
    }

    /** {@code <}, {@code <=}, {@code >}, {@code >=}: compara dados, nao testa uma marca. */
    static boolean ehRelacional(Expression condicao) {
        return semParenteses(condicao) instanceof BinaryExpr binaria && RELACIONAIS.contains(binaria.getOperator());
    }

    private static boolean garanteQuandoVerdadeira(Expression nua) {
        if (nua instanceof BinaryExpr binaria) {
            return binaria.getOperator() != BinaryExpr.Operator.OR;
        }
        return !(negacaoDe(nua) instanceof BinaryExpr binaria && binaria.getOperator() == BinaryExpr.Operator.AND);
    }

    private static boolean garanteQuandoFalsa(Expression nua) {
        if (nua instanceof BinaryExpr binaria) {
            return binaria.getOperator() != BinaryExpr.Operator.AND;
        }
        return !(negacaoDe(nua) instanceof BinaryExpr binaria && binaria.getOperator() == BinaryExpr.Operator.OR);
    }

    /** O que a negacao nega ({@code !(a && b)} da {@code a && b}); sem negacao, a propria expressao. */
    private static Expression negacaoDe(Expression nua) {
        if (nua instanceof UnaryExpr negacao && negacao.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
            return semParenteses(negacao.getExpression());
        }
        return nua;
    }

    private static List<Expression> unir(List<Expression> esquerda, List<Expression> direita) {
        List<Expression> todas = new ArrayList<>(esquerda);
        todas.addAll(direita);
        return todas;
    }

    private static Expression semParenteses(Expression expressao) {
        Expression atual = expressao;
        while (atual instanceof EnclosedExpr entre) {
            atual = entre.getInner();
        }
        return atual;
    }
}
