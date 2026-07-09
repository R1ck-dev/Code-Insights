package com.projeto.codeinsights.infrastructure.metrica;

import org.springframework.stereotype.Component;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;

/**
 * Complexidade Ciclomatica de McCabe: numero de caminhos independentes do fluxo
 * de controle. Metrica <b>exata</b>, nao heuristica - dai a confianca sempre ALTA.
 * Para um programa com P metodos, M = (pontos de decisao) + P, equivalente a somar
 * (decisoes + 1) por metodo. Contam como decisao: if, for/foreach, while, do-while,
 * cada rotulo de case, catch, expressao ternaria e os operadores de curto-circuito
 * {@code &&} e {@code ||}.
 */
@Component
class CiclomaticaAnalisador implements AnalisadorMetricaJava {

    @Override
    public TipoMetrica tipo() {
        return TipoMetrica.COMPLEXIDADE_CICLOMATICA;
    }

    @Override
    public MetricaCalculada analisar(CompilationUnit unidade) {
        int decisoes = pontosDeDecisao(unidade);
        int componentes = Math.max(unidade.findAll(MethodDeclaration.class).size()
                + unidade.findAll(ConstructorDeclaration.class).size(), 1);

        int complexidade = decisoes + componentes;
        String detalhe = "%d ponto(s) de decisao em %d metodo(s)/construtor(es) (M = decisoes + P); "
                .formatted(decisoes, componentes)
                + "metodo mais ramificado = %d.".formatted(maiorPorMetodo(unidade));
        return new MetricaCalculada(complexidade, String.valueOf(complexidade), detalhe, NivelConfianca.ALTA);
    }

    /** O total esconde metodos individualmente complexos; o maximo por metodo e o que a literatura reporta. */
    private int maiorPorMetodo(CompilationUnit unidade) {
        return unidade.findAll(MethodDeclaration.class).stream()
                .mapToInt(metodo -> pontosDeDecisao(metodo) + 1)
                .max()
                .orElse(1);
    }

    private int pontosDeDecisao(Node no) {
        return no.findAll(IfStmt.class).size()
                + no.findAll(ForStmt.class).size()
                + no.findAll(ForEachStmt.class).size()
                + no.findAll(WhileStmt.class).size()
                + no.findAll(DoStmt.class).size()
                + no.findAll(CatchClause.class).size()
                + no.findAll(ConditionalExpr.class).size()
                + no.findAll(SwitchEntry.class).stream()
                        .mapToInt(entrada -> entrada.getLabels().size())
                        .sum()
                + (int) no.findAll(BinaryExpr.class).stream()
                        .filter(this::ehOperadorLogico)
                        .count();
    }

    private boolean ehOperadorLogico(BinaryExpr expressao) {
        return expressao.getOperator() == BinaryExpr.Operator.AND
                || expressao.getOperator() == BinaryExpr.Operator.OR;
    }
}
