package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.HashSet;
import java.util.Set;

import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

/**
 * Como o codigo le e escreve num array, mapa ou conjunto candidato a cache. Compartilhado por
 * {@link ClassificadorDeCache} (a consulta que poda) e {@link DesmarcacaoNaVolta} (a escrita
 * que desfaz), para que os dois concordem sobre o que e acesso ao cache.
 */
final class AcessosAoCache {

    private static final Set<String> CONSULTAS_DE_CACHE = Set.of("get", "containsKey", "contains", "getOrDefault");

    private AcessosAoCache() {
    }

    /** A expressao consulta o cache pela chave: {@code memo[n]}, {@code memo.get(n)}, {@code vis.contains(v)}. */
    static boolean consulta(Expression expressao, String cache) {
        boolean porIndice = expressao.findAll(ArrayAccessExpr.class).stream()
                .anyMatch(acesso -> AstUtils.referenciaAlguma(acesso.getName(), Set.of(cache)));
        boolean porConsulta = expressao.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> CONSULTAS_DE_CACHE.contains(chamada.getNameAsString()))
                .anyMatch(chamada -> temEscopo(chamada, cache));
        return porIndice || porConsulta;
    }

    /**
     * Os nomes que formam a chave da consulta: {@code i} e {@code j} em {@code grade[i - 1][j]},
     * {@code n} em {@code memo.containsKey(n)}. O proprio nome do cache fica de fora.
     */
    static Set<String> nomesDaChave(Expression expressao, String cache) {
        Set<String> nomes = new HashSet<>();
        expressao.findAll(ArrayAccessExpr.class).stream()
                .filter(acesso -> AstUtils.referenciaAlguma(acesso.getName(), Set.of(cache)))
                .forEach(acesso -> acrescentarNomes(acesso.getIndex(), nomes));
        expressao.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> CONSULTAS_DE_CACHE.contains(chamada.getNameAsString()))
                .filter(chamada -> temEscopo(chamada, cache))
                .forEach(chamada -> chamada.getArguments().forEach(argumento -> acrescentarNomes(argumento, nomes)));
        nomes.remove(cache);
        return nomes;
    }

    /** Os nomes que uma expressao menciona, no proprio no e abaixo dele. */
    static Set<String> nomesEm(Expression expressao) {
        Set<String> nomes = new HashSet<>();
        acrescentarNomes(expressao, nomes);
        return nomes;
    }

    private static void acrescentarNomes(Expression expressao, Set<String> nomes) {
        expressao.findAll(NameExpr.class).forEach(nome -> nomes.add(nome.getNameAsString()));
    }

    /** O alvo de uma atribuicao e o cache ({@code memo = ...}) ou um elemento dele ({@code memo[n] = ...}). */
    static boolean escreveEm(Expression alvo, String cache) {
        if (alvo instanceof ArrayAccessExpr acesso) {
            return AstUtils.referenciaAlguma(acesso.getName(), Set.of(cache));
        }
        return alvo instanceof NameExpr nome && nome.getNameAsString().equals(cache);
    }

    /** {@code cache.metodo(...)}, com o cache nomeado diretamente como receptor. */
    static boolean temEscopo(MethodCallExpr chamada, String cache) {
        return chamada.getScope()
                .map(escopo -> escopo instanceof NameExpr nome && nome.getNameAsString().equals(cache))
                .orElse(false);
    }
}
