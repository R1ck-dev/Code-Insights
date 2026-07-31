package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

/** Predicados de AST compartilhados pelo motor de custo. */
public final class AstUtils {

    private AstUtils() {
    }

    public static boolean ehLaco(Node no) {
        return no instanceof ForStmt || no instanceof ForEachStmt
                || no instanceof WhileStmt || no instanceof DoStmt;
    }

    /** Laco mais proximo que envolve {@code no}, ou {@code null} se nenhum. */
    public static Node lacoMaisProximo(Node no) {
        Node pai = no.getParentNode().orElse(null);
        while (pai != null) {
            if (ehLaco(pai)) {
                return pai;
            }
            pai = pai.getParentNode().orElse(null);
        }
        return null;
    }

    /** Chave que identifica um metodo dentro da unidade de compilacao (nome + aridade). */
    public static String chaveDoMetodo(MethodDeclaration metodo) {
        return metodo.getNameAsString() + "/" + metodo.getParameters().size();
    }

    /** {@code true} se a chamada e ao proprio metodo (sem escopo ou via {@code this}). */
    public static boolean ehAutoChamada(MethodCallExpr chamada, MethodDeclaration metodo) {
        if (!chamada.getNameAsString().equals(metodo.getNameAsString())
                || chamada.getArguments().size() != metodo.getParameters().size()) {
            return false;
        }
        return chamada.getScope().map(escopo -> escopo instanceof ThisExpr).orElse(true);
    }

    /**
     * {@code true} se todo caminho de execucao por {@code comando} termina em
     * {@code return}/{@code throw} - ou seja, o codigo apos ele e inalcancavel.
     * E o que permite contar auto-chamadas por caminho, e nao por ocorrencia.
     */
    public static boolean sempreRetorna(Statement comando) {
        if (comando instanceof ReturnStmt || comando instanceof ThrowStmt) {
            return true;
        }
        if (comando instanceof BlockStmt bloco) {
            return bloco.getStatements().stream().anyMatch(AstUtils::sempreRetorna);
        }
        if (comando instanceof IfStmt condicional) {
            return condicional.getElseStmt()
                    .map(senao -> sempreRetorna(condicional.getThenStmt()) && sempreRetorna(senao))
                    .orElse(false);
        }
        return false;
    }

    /** A expressao contem uma divisao por constante ({@code x / 2}, {@code x >> 1})? */
    public static boolean contemDivisaoPorConstante(Expression expressao) {
        return expressao.findAll(BinaryExpr.class).stream().anyMatch(AstUtils::ehDivisaoPorConstante);
    }

    private static boolean ehDivisaoPorConstante(BinaryExpr binaria) {
        boolean operadorDivide = binaria.getOperator() == BinaryExpr.Operator.DIVIDE
                || binaria.getOperator() == BinaryExpr.Operator.SIGNED_RIGHT_SHIFT
                || binaria.getOperator() == BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT;
        return operadorDivide && binaria.getRight().isIntegerLiteralExpr();
    }

    /** A expressao le alguma das variaveis de {@code nomes}? */
    public static boolean referenciaAlguma(Node no, Set<String> nomes) {
        return no.findAll(NameExpr.class).stream()
                .anyMatch(referencia -> nomes.contains(referencia.getNameAsString()));
    }

    /**
     * Nome da variavel que uma atribuicao <b>acumula sobre si mesma</b>, nas duas formas que
     * significam a mesma coisa: {@code s += x} e {@code s = s + x}.
     * <p>
     * As duas precisam ser reconhecidas porque o iniciante escreve a segunda, e ler so a
     * primeira fazia o motor <b>subestimar</b> a concatenacao de String em laco — dizer O(n)
     * onde o custo e O(n^2). Cabe aqui, e nao em cada avaliador, para que tempo e espaco nao
     * possam divergir sobre o que conta como acumulo.
     * <p>
     * Nao filtra por tipo: quem chama decide se o alvo e uma {@code String} (acumulo caro) ou
     * um {@code int} (acumulo O(1)).
     */
    public static Optional<String> alvoDeAcumulacao(AssignExpr atribuicao) {
        if (!atribuicao.getTarget().isNameExpr()) {
            return Optional.empty();
        }
        String alvo = atribuicao.getTarget().asNameExpr().getNameAsString();

        if (atribuicao.getOperator() == AssignExpr.Operator.PLUS) {
            return Optional.of(alvo);
        }
        boolean somaQueSeReferencia = atribuicao.getOperator() == AssignExpr.Operator.ASSIGN
                && atribuicao.getValue() instanceof BinaryExpr soma
                && soma.getOperator() == BinaryExpr.Operator.PLUS
                && referenciaAlguma(soma, Set.of(alvo));
        return somaQueSeReferencia ? Optional.of(alvo) : Optional.empty();
    }
}
