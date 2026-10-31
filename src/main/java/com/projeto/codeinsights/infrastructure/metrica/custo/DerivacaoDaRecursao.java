package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

/**
 * Decide se um valor <b>vem da recursao</b>: contem uma auto-chamada ou le uma local "resultado",
 * isto e, uma local que recebeu uma auto-chamada (ou algo calculado de outra local resultado,
 * por ponto fixo). Compartilhado por {@link DesmarcacaoNaVolta} (a escrita que grava o resultado
 * nunca e o valor livre) e {@link ProtecaoPorEnvolvimento} (o ramo que escreve o resultado no cache).
 */
final class DerivacaoDaRecursao {

    /** Uma local recebe um valor: na declaracao ({@code int t = e}) ou numa atribuicao ({@code t = e}). */
    record Atribuicao(String local, Expression valor) {
    }

    private final Set<MethodCallExpr> autoChamadas = ResolvedorDeChamadas.conjuntoPorIdentidade();
    private final List<Atribuicao> atribuicoes;
    private final Set<String> locaisResultado;

    DerivacaoDaRecursao(MethodDeclaration metodo, Collection<MethodCallExpr> autoChamadas) {
        this.autoChamadas.addAll(autoChamadas);
        this.atribuicoes = atribuicoesALocais(metodo);
        this.locaisResultado = locaisResultado();
    }

    /** O valor contem uma auto-chamada ou le uma local resultado. */
    boolean deriva(Expression valor) {
        return deriva(valor, locaisResultado);
    }

    List<Atribuicao> atribuicoes() {
        return atribuicoes;
    }

    private static List<Atribuicao> atribuicoesALocais(MethodDeclaration metodo) {
        List<Atribuicao> achadas = new ArrayList<>();
        for (VariableDeclarator declarada : metodo.findAll(VariableDeclarator.class)) {
            declarada.getInitializer()
                    .ifPresent(inicial -> achadas.add(new Atribuicao(declarada.getNameAsString(), inicial)));
        }
        for (AssignExpr atribuicao : metodo.findAll(AssignExpr.class)) {
            if (atribuicao.getTarget() instanceof NameExpr alvo) {
                achadas.add(new Atribuicao(alvo.getNameAsString(), atribuicao.getValue()));
            }
        }
        return achadas;
    }

    private Set<String> locaisResultado() {
        Set<String> derivados = new HashSet<>();
        boolean cresceu = true;
        while (cresceu) {
            cresceu = false;
            for (Atribuicao atribuicao : atribuicoes) {
                if (deriva(atribuicao.valor(), derivados) && derivados.add(atribuicao.local())) {
                    cresceu = true;
                }
            }
        }
        return derivados;
    }

    private boolean deriva(Expression valor, Set<String> derivados) {
        boolean chamaASiMesmo = valor.findAll(MethodCallExpr.class).stream().anyMatch(autoChamadas::contains);
        return chamaASiMesmo || AstUtils.referenciaAlguma(valor, derivados);
    }
}
