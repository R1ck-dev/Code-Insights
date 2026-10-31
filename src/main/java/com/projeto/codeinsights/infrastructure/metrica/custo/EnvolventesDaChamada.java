package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.projeto.codeinsights.infrastructure.metrica.custo.UsoDeCache.Forma;

/**
 * As construcoes que <b>condicionam</b> uma auto-chamada, da mais interna para a mais externa:
 * o {@code then} de um {@code if}, um ramo de ternario e o que vem depois de {@code if (...) continue;}
 * no corpo de um laco. Cada uma entrega as condicoes que a chamada tem garantidas ao executar.
 */
final class EnvolventesDaChamada {

    /**
     * Uma construcao que condiciona a chamada.
     *
     * @param garantias o que vale sempre que a chamada executa (ver {@link CondicaoDeRamo})
     * @param ramo      o ramo que contem a chamada, onde se procura a escrita do resultado; {@code null}
     *                  quando nao ha ramo (o {@code continue} apenas pula)
     * @param seGrava   a forma quando o ramo grava no cache um valor derivado da recursao
     * @param seSoChecar a forma quando o ramo apenas chama, e a consulta e a unica prova
     */
    record Protecao(List<Expression> garantias, Node ramo, Forma seGrava, Forma seSoChecar) {
    }

    private EnvolventesDaChamada() {
    }

    static List<Protecao> de(MethodCallExpr chamada) {
        List<Protecao> protecoes = new ArrayList<>();
        boolean cruzouLaco = false;
        Node filho = chamada;
        while (filho.getParentNode().isPresent() && !(filho instanceof MethodDeclaration)) {
            Node pai = filho.getParentNode().get();
            protecaoDoIf(pai, filho).ifPresent(protecoes::add);
            protecaoDoTernario(pai, filho).ifPresent(protecoes::add);
            if (!cruzouLaco && pai instanceof BlockStmt bloco) {
                protecoes.addAll(protecoesDoContinue(bloco, filho));
            }
            cruzouLaco = cruzouLaco || AstUtils.ehLaco(pai) || pai instanceof LambdaExpr;
            filho = pai;
        }
        return protecoes;
    }

    private static Optional<Protecao> protecaoDoIf(Node pai, Node filho) {
        if (pai instanceof IfStmt condicional && condicional.getThenStmt() == filho) {
            return Optional.of(new Protecao(CondicaoDeRamo.verdadeiras(condicional.getCondition()), filho,
                    Forma.CALCULA_SE_AUSENTE, Forma.CHECA_ANTES_DE_CHAMAR));
        }
        return Optional.empty();
    }

    /** O ramo "then" so executa com a condicao verdadeira; o "else", com ela falsa. */
    private static Optional<Protecao> protecaoDoTernario(Node pai, Node filho) {
        if (!(pai instanceof ConditionalExpr ternario)) {
            return Optional.empty();
        }
        if (ternario.getThenExpr() == filho) {
            return Optional.of(ternaria(CondicaoDeRamo.verdadeiras(ternario.getCondition()), filho));
        }
        if (ternario.getElseExpr() == filho) {
            return Optional.of(ternaria(CondicaoDeRamo.falsas(ternario.getCondition()), filho));
        }
        return Optional.empty();
    }

    private static Protecao ternaria(List<Expression> garantias, Node ramo) {
        return new Protecao(garantias, ramo, Forma.TERNARIO, Forma.CHECA_ANTES_DE_CHAMAR);
    }

    /**
     * Os {@code if (...) continue;} que vem antes de {@code seguinte} no corpo de um laco: o
     * {@code continue} sem rotulo pula o resto do corpo, entao a chamada so executa com a condicao falsa.
     */
    private static List<Protecao> protecoesDoContinue(BlockStmt bloco, Node seguinte) {
        if (bloco.getParentNode().filter(AstUtils::ehLaco).isEmpty()) {
            return List.of();
        }
        List<Protecao> protecoes = new ArrayList<>();
        for (Statement comando : bloco.getStatements()) {
            if (comando == seguinte) {
                break;
            }
            if (comando instanceof IfStmt condicional && condicional.getElseStmt().isEmpty()
                    && pulaOLaco(condicional.getThenStmt())) {
                protecoes.add(new Protecao(CondicaoDeRamo.falsas(condicional.getCondition()), null,
                        Forma.NENHUMA, Forma.PULA_NO_LACO));
            }
        }
        return protecoes;
    }

    /** {@code continue;} sem rotulo, sozinho ou como ultimo comando de um bloco. */
    private static boolean pulaOLaco(Statement ramo) {
        if (ramo instanceof ContinueStmt pula) {
            return pula.getLabel().isEmpty();
        }
        if (ramo instanceof BlockStmt bloco && !bloco.getStatements().isEmpty()) {
            return pulaOLaco(bloco.getStatements().get(bloco.getStatements().size() - 1));
        }
        return false;
    }
}
