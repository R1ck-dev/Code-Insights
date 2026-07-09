package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

/**
 * Quantas vezes um laco executa, em funcao do tamanho da entrada.
 * <p>
 * O sinal de um laco logaritmico nao e "a variavel de controle e multiplicada" -
 * essa regra so pega {@code i *= 2} e perde o idioma real da busca binaria, onde
 * {@code lo} e {@code hi} se aproximam por atribuicoes <b>aditivas</b>
 * ({@code lo = mid + 1}). O sinal correto e mais geral: <b>o intervalo que governa
 * a parada e dividido a cada volta</b>. Reconhecemos as duas formas:
 * <ul>
 *   <li>indice multiplicativo: a variavel da condicao e atualizada por {@code *=},
 *       {@code /=}, {@code >>=} (ou {@code i = i / 2});</li>
 *   <li>intervalo que encolhe: existe um ponto medio - variavel do corpo cujo valor
 *       vem de uma divisao por constante sobre as variaveis da condicao - e alguma
 *       dessas variaveis e reatribuida a partir dele.</li>
 * </ul>
 * Acumuladores no corpo ({@code fat *= i}) nao contam: nao controlam a parada.
 */
public final class IteracoesDeLaco {

    private IteracoesDeLaco() {
    }

    public static CustoAvaliado de(Node laco) {
        if (laco instanceof ForEachStmt) {
            return CustoAvaliado.exato(Custo.LINEAR);
        }
        Set<String> variaveisDeTerminacao = variaveisDaCondicao(laco);
        if (variaveisDeTerminacao.isEmpty()) {
            return CustoAvaliado.estimado(Custo.LINEAR, "laco sem condicao de parada; assumidas n iteracoes");
        }
        if (ehLimitadoPorConstante(laco)) {
            return CustoAvaliado.exato(Custo.CONSTANTE);
        }
        if (ehLogaritmico(laco, variaveisDeTerminacao)) {
            return CustoAvaliado.exato(Custo.LOGARITMICO);
        }
        if (temPassoUnitario(laco, variaveisDeTerminacao)) {
            return CustoAvaliado.exato(Custo.LINEAR);
        }
        return CustoAvaliado.estimado(Custo.LINEAR, "progressao do laco nao reconhecida; assumidas n iteracoes");
    }

    /** {@code for (int i = 0; i < 26; i++)} nao depende da entrada: O(1). */
    private static boolean ehLimitadoPorConstante(Node laco) {
        if (!(laco instanceof ForStmt paraLaco)) {
            return false;
        }
        boolean iniciosLiterais = paraLaco.getInitialization().stream()
                .flatMap(inicio -> inicio.findAll(VariableDeclarator.class).stream())
                .allMatch(declaracao -> declaracao.getInitializer().map(Expression::isIntegerLiteralExpr).orElse(false));
        return iniciosLiterais && !paraLaco.getInitialization().isEmpty()
                && paraLaco.getCompare().map(IteracoesDeLaco::comparaComLiteral).orElse(false);
    }

    private static boolean comparaComLiteral(Expression condicao) {
        if (!(condicao instanceof BinaryExpr binaria)) {
            return false;
        }
        return binaria.getLeft().isIntegerLiteralExpr() || binaria.getRight().isIntegerLiteralExpr();
    }

    private static boolean ehLogaritmico(Node laco, Set<String> variaveisDeTerminacao) {
        return temIndiceMultiplicativo(laco, variaveisDeTerminacao)
                || temIntervaloQueEncolhe(laco, variaveisDeTerminacao);
    }

    private static boolean temIndiceMultiplicativo(Node laco, Set<String> variaveisDeTerminacao) {
        return atribuicoesDoLaco(laco)
                .filter(IteracoesDeLaco::ehAtualizacaoMultiplicativa)
                .anyMatch(atribuicao -> variaveisDeTerminacao.contains(nomeDoAlvo(atribuicao)));
    }

    /**
     * Busca binaria e afins: {@code mid} nasce de uma divisao sobre {@code lo}/{@code hi}
     * (as variaveis da condicao) e alguma delas passa a valer algo derivado de {@code mid}.
     */
    private static boolean temIntervaloQueEncolhe(Node laco, Set<String> variaveisDeTerminacao) {
        Set<String> pontosMedios = pontosMedios(laco, variaveisDeTerminacao);
        if (pontosMedios.isEmpty()) {
            return false;
        }
        return atribuicoesDoLaco(laco)
                .filter(atribuicao -> variaveisDeTerminacao.contains(nomeDoAlvo(atribuicao)))
                .anyMatch(atribuicao -> AstUtils.referenciaAlguma(atribuicao.getValue(), pontosMedios));
    }

    private static Set<String> pontosMedios(Node laco, Set<String> variaveisDeTerminacao) {
        Set<String> medios = new HashSet<>();
        laco.findAll(VariableDeclarator.class).stream()
                .filter(declaracao -> AstUtils.lacoMaisProximo(declaracao) == laco)
                .forEach(declaracao -> declaracao.getInitializer()
                        .filter(valor -> ehDivisaoSobre(valor, variaveisDeTerminacao))
                        .ifPresent(valor -> medios.add(declaracao.getNameAsString())));
        atribuicoesDoLaco(laco)
                .filter(atribuicao -> ehDivisaoSobre(atribuicao.getValue(), variaveisDeTerminacao))
                .forEach(atribuicao -> medios.add(nomeDoAlvo(atribuicao)));
        medios.remove(null);
        return medios;
    }

    private static boolean ehDivisaoSobre(Expression valor, Set<String> variaveisDeTerminacao) {
        return AstUtils.contemDivisaoPorConstante(valor) && AstUtils.referenciaAlguma(valor, variaveisDeTerminacao);
    }

    /** {@code i++}, {@code i--}, {@code i += k} sobre uma variavel da condicao: n iteracoes, com certeza. */
    private static boolean temPassoUnitario(Node laco, Set<String> variaveisDeTerminacao) {
        boolean porUnario = laco.findAll(UnaryExpr.class).stream()
                .filter(unario -> AstUtils.lacoMaisProximo(unario) == laco)
                .filter(IteracoesDeLaco::ehIncrementoOuDecremento)
                .anyMatch(unario -> unario.getExpression().isNameExpr()
                        && variaveisDeTerminacao.contains(unario.getExpression().asNameExpr().getNameAsString()));
        if (porUnario) {
            return true;
        }
        return atribuicoesDoLaco(laco)
                .filter(atribuicao -> atribuicao.getOperator() == AssignExpr.Operator.PLUS
                        || atribuicao.getOperator() == AssignExpr.Operator.MINUS)
                .anyMatch(atribuicao -> variaveisDeTerminacao.contains(nomeDoAlvo(atribuicao)));
    }

    private static boolean ehIncrementoOuDecremento(UnaryExpr unario) {
        return switch (unario.getOperator()) {
            case PREFIX_INCREMENT, POSTFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_DECREMENT -> true;
            default -> false;
        };
    }

    /** Atribuicoes cujo laco envolvente imediato e {@code laco} (ignora as de lacos internos). */
    private static Stream<AssignExpr> atribuicoesDoLaco(Node laco) {
        return laco.findAll(AssignExpr.class).stream()
                .filter(atribuicao -> AstUtils.lacoMaisProximo(atribuicao) == laco);
    }

    private static Set<String> variaveisDaCondicao(Node laco) {
        return condicao(laco)
                .map(expressao -> expressao.findAll(NameExpr.class).stream()
                        .map(NameExpr::getNameAsString)
                        .collect(Collectors.toSet()))
                .orElseGet(Set::of);
    }

    private static Optional<Expression> condicao(Node laco) {
        if (laco instanceof ForStmt paraLaco) {
            return paraLaco.getCompare();
        }
        if (laco instanceof WhileStmt enquantoLaco) {
            return Optional.of(enquantoLaco.getCondition());
        }
        if (laco instanceof DoStmt facaLaco) {
            return Optional.of(facaLaco.getCondition());
        }
        return Optional.empty();
    }

    private static String nomeDoAlvo(AssignExpr atribuicao) {
        return atribuicao.getTarget().isNameExpr()
                ? atribuicao.getTarget().asNameExpr().getNameAsString()
                : null;
    }

    private static boolean ehAtualizacaoMultiplicativa(AssignExpr atribuicao) {
        if (ehOperadorCompostoMultiplicativo(atribuicao.getOperator())) {
            return true;
        }
        if (atribuicao.getOperator() != AssignExpr.Operator.ASSIGN || !atribuicao.getValue().isBinaryExpr()) {
            return false;
        }
        BinaryExpr binaria = atribuicao.getValue().asBinaryExpr();
        return ehOperadorBinarioMultiplicativo(binaria.getOperator())
                && referenciaOProprioAlvo(atribuicao);
    }

    private static boolean ehOperadorCompostoMultiplicativo(AssignExpr.Operator operador) {
        return switch (operador) {
            case MULTIPLY, DIVIDE, LEFT_SHIFT, SIGNED_RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT -> true;
            default -> false;
        };
    }

    private static boolean ehOperadorBinarioMultiplicativo(BinaryExpr.Operator operador) {
        return switch (operador) {
            case MULTIPLY, DIVIDE, LEFT_SHIFT, SIGNED_RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT -> true;
            default -> false;
        };
    }

    /** {@code i = i * 2} conta; {@code total = a * b} nao. */
    private static boolean referenciaOProprioAlvo(AssignExpr atribuicao) {
        String alvo = nomeDoAlvo(atribuicao);
        return alvo != null && AstUtils.referenciaAlguma(atribuicao.getValue(), Set.of(alvo));
    }
}
