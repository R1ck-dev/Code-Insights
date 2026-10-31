package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ForStmt;

/**
 * {@code for (int i = 0; i < 26; i++)} nao depende da entrada: O(1).
 * <p>
 * O sinal nao e "a condicao tem um literal" - {@code for (int p = 0; trocou == 1; p++)} tem
 * literal e roda n vezes. O laco e constante so quando a variavel comparada na condicao e o
 * <b>contador do cabecalho</b>: ele nasce constante no init (declarador ou atribuicao), e
 * comparado com um limite constante (em qualquer lado) e avanca no update do proprio {@code for}.
 * Constante e literal inteiro, {@code -literal}, literal {@code char} ou nome de constante final
 * ({@link ConstantesFinais}). Escrita no contador dentro do corpo nao e conferida.
 */
final class LacoLimitadoPorConstante {

    private LacoLimitadoPorConstante() {
    }

    static boolean de(ForStmt laco, ConstantesFinais constantes) {
        Predicate<Expression> ehConstante = constantes::ehConstante;
        return laco.getCompare()
                .filter(BinaryExpr.class::isInstance)
                .map(condicao -> (BinaryExpr) condicao)
                .filter(LacoLimitadoPorConstante::ehComparacaoDeOrdem)
                .flatMap(condicao -> contadorComparado(condicao, ehConstante))
                .map(contador -> contadoresDoInit(laco, ehConstante).contains(contador) && avancaNoUpdate(laco, contador))
                .orElse(false);
    }

    private static boolean ehComparacaoDeOrdem(BinaryExpr condicao) {
        return switch (condicao.getOperator()) {
            case LESS, LESS_EQUALS, GREATER, GREATER_EQUALS, NOT_EQUALS -> true;
            default -> false;
        };
    }

    /** O nome da variavel de um lado da comparacao quando o outro lado e um limite constante. */
    private static Optional<String> contadorComparado(BinaryExpr condicao, Predicate<Expression> ehConstante) {
        if (condicao.getLeft().isNameExpr() && ehConstante.test(condicao.getRight())) {
            return Optional.of(condicao.getLeft().asNameExpr().getNameAsString());
        }
        if (condicao.getRight().isNameExpr() && ehConstante.test(condicao.getLeft())) {
            return Optional.of(condicao.getRight().asNameExpr().getNameAsString());
        }
        return Optional.empty();
    }

    /** Variaveis que o init do {@code for} faz nascer com valor constante. */
    private static Set<String> contadoresDoInit(ForStmt laco, Predicate<Expression> ehConstante) {
        Set<String> contadores = new HashSet<>();
        for (Expression inicio : laco.getInitialization()) {
            if (inicio instanceof VariableDeclarationExpr declaracao) {
                declaracao.getVariables().stream()
                        .filter(variavel -> variavel.getInitializer().map(ehConstante::test).orElse(false))
                        .forEach(variavel -> contadores.add(variavel.getNameAsString()));
            } else if (inicio instanceof AssignExpr atribuicao
                    && atribuicao.getOperator() == AssignExpr.Operator.ASSIGN
                    && atribuicao.getTarget().isNameExpr()
                    && ehConstante.test(atribuicao.getValue())) {
                contadores.add(atribuicao.getTarget().asNameExpr().getNameAsString());
            }
        }
        return contadores;
    }

    /** {@code i++}, {@code --i}, {@code i += k}, {@code i *= 2}... no update do proprio cabecalho. */
    private static boolean avancaNoUpdate(ForStmt laco, String contador) {
        return laco.getUpdate().stream().anyMatch(passo -> atualiza(passo, contador));
    }

    private static boolean atualiza(Expression passo, String contador) {
        if (passo instanceof UnaryExpr unario) {
            return ehIncrementoOuDecremento(unario) && ehVariavel(unario.getExpression(), contador);
        }
        return passo instanceof AssignExpr atribuicao && ehVariavel(atribuicao.getTarget(), contador);
    }

    private static boolean ehIncrementoOuDecremento(UnaryExpr unario) {
        return switch (unario.getOperator()) {
            case PREFIX_INCREMENT, POSTFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_DECREMENT -> true;
            default -> false;
        };
    }

    private static boolean ehVariavel(Expression expressao, String nome) {
        return expressao.isNameExpr() && expressao.asNameExpr().getNameAsString().equals(nome);
    }
}
