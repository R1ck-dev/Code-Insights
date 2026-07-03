package com.projeto.codeinsights.infrastructure.metrica;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

/** Utilidades de AST compartilhadas pelos analisadores de complexidade. */
final class AstUtils {

    private AstUtils() {
    }

    static boolean ehLaco(Node no) {
        return no instanceof ForStmt || no instanceof ForEachStmt
                || no instanceof WhileStmt || no instanceof DoStmt;
    }

    /** Laco mais proximo que envolve {@code no}, ou {@code null} se nenhum. */
    static Node lacoMaisProximo(Node no) {
        Node pai = no.getParentNode().orElse(null);
        while (pai != null) {
            if (ehLaco(pai)) {
                return pai;
            }
            pai = pai.getParentNode().orElse(null);
        }
        return null;
    }

    /**
     * Grau de recursao direta: 2 se algum metodo se chama 2+ vezes (arvore de
     * recursao ~exponencial), 1 se ha recursao linear (uma auto-chamada), 0 se
     * nenhum metodo recorre. Heuristica por nome (ignora sobrecarga).
     */
    static int grauDeRecursao(CompilationUnit unidade) {
        int grau = 0;
        for (MethodDeclaration metodo : unidade.findAll(MethodDeclaration.class)) {
            long autoChamadas = metodo.findAll(MethodCallExpr.class).stream()
                    .filter(chamada -> chamada.getNameAsString().equals(metodo.getNameAsString()))
                    .count();
            if (autoChamadas >= 2) {
                return 2;
            }
            if (autoChamadas == 1) {
                grau = 1;
            }
        }
        return grau;
    }
}
