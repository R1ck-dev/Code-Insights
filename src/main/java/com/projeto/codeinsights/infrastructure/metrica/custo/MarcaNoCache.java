package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

/**
 * O metodo <b>marca</b> o cache? Uma marca e uma escrita cujo valor <b>nao e copia de outra celula</b>
 * do proprio cache: {@code vis[u] = true}, {@code grade[i][j] = '0'}, {@code dist[v] = dist[u] + 1},
 * {@code visitados.add(u)}. Trocar ou deslocar celulas ({@code a[i] = a[j]}, {@code a[j] = t} com
 * {@code t = a[i]}) reordena dados e nao registra que um estado foi visitado.
 * <p>
 * E o que separa a DFS com visitados, cuja consulta so poda porque o metodo marca, de
 * {@code if (a[i] != a[j]) { troca; ordena(a, i + 1, j); }}, em que a consulta compara dados e a
 * recursao nao tem estado para repetir. Exigido das formas em que a consulta e a unica prova de que
 * a chamada e podada (checa-antes e pula-no-laco).
 */
final class MarcaNoCache {

    private final String cache;
    private final Set<String> copiasDoCache;

    private MarcaNoCache(String cache, DerivacaoDaRecursao recursao) {
        this.cache = cache;
        this.copiasDoCache = locaisQueCopiam(recursao.atribuicoes());
    }

    static boolean escreve(MethodDeclaration metodo, String cache, DerivacaoDaRecursao recursao) {
        return new MarcaNoCache(cache, recursao).existeEm(metodo);
    }

    private boolean existeEm(MethodDeclaration metodo) {
        boolean porAtribuicao = metodo.findAll(AssignExpr.class).stream()
                .filter(atribuicao -> atribuicao.getOperator() == AssignExpr.Operator.ASSIGN)
                .filter(atribuicao -> AcessosAoCache.escreveEm(atribuicao.getTarget(), cache))
                .anyMatch(atribuicao -> !ehCopia(atribuicao.getValue()));
        boolean porChamada = metodo.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> AcessosAoCache.temEscopo(chamada, cache))
                .anyMatch(this::marcaPorChamada);
        return porAtribuicao || porChamada;
    }

    /** {@code add(x)} marca; {@code put(k, v)} marca se {@code v} nao copia o cache. */
    private boolean marcaPorChamada(MethodCallExpr chamada) {
        return switch (chamada.getNameAsString()) {
            case "add" -> true;
            case "put" -> chamada.getArguments().size() == 2 && !ehCopia(chamada.getArgument(1));
            default -> false;
        };
    }

    // ----- copia de uma celula do cache -----

    /** O valor e uma celula do cache ({@code a[j]}, {@code m.get(k)}) ou uma local que guarda uma. */
    private boolean ehCopia(Expression valor) {
        Expression nua = semParenteses(valor);
        return leCelula(nua) || (nua instanceof NameExpr nome && copiasDoCache.contains(nome.getNameAsString()));
    }

    private boolean leCelula(Expression nua) {
        if (nua instanceof ArrayAccessExpr acesso) {
            return AstUtils.referenciaAlguma(acesso.getName(), Set.of(cache));
        }
        return nua instanceof MethodCallExpr chamada
                && chamada.getNameAsString().equals("get") && AcessosAoCache.temEscopo(chamada, cache);
    }

    private Set<String> locaisQueCopiam(List<DerivacaoDaRecursao.Atribuicao> atribuicoes) {
        Set<String> locais = new HashSet<>();
        atribuicoes.stream()
                .filter(atribuicao -> leCelula(semParenteses(atribuicao.valor())))
                .forEach(atribuicao -> locais.add(atribuicao.local()));
        return locais;
    }

    private static Expression semParenteses(Expression expressao) {
        Expression atual = expressao;
        while (atual instanceof EnclosedExpr entre) {
            atual = entre.getInner();
        }
        return atual;
    }
}
