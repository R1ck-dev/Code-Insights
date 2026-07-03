package com.projeto.codeinsights.infrastructure.metrica;

import org.springframework.stereotype.Component;

import com.github.javaparser.ast.CompilationUnit;
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
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;

/**
 * Complexidade Ciclomatica de McCabe: numero de caminhos independentes do fluxo
 * de controle. Metrica exata (nao heuristica). Para um programa com P metodos,
 * M = (pontos de decisao) + P, equivalente a somar (decisoes + 1) por metodo.
 * Contam como decisao: if, for/foreach, while, do-while, cada rotulo de case,
 * catch, expressao ternaria e os operadores logicos de curto-circuito && e ||.
 */
@Component
class CiclomaticaAnalisador implements AnalisadorMetricaJava {

    @Override
    public TipoMetrica tipo() {
        return TipoMetrica.COMPLEXIDADE_CICLOMATICA;
    }

    @Override
    public MetricaCalculada analisar(CompilationUnit unidade) {
        int decisoes = unidade.findAll(IfStmt.class).size()
                + unidade.findAll(ForStmt.class).size()
                + unidade.findAll(ForEachStmt.class).size()
                + unidade.findAll(WhileStmt.class).size()
                + unidade.findAll(DoStmt.class).size()
                + unidade.findAll(CatchClause.class).size()
                + unidade.findAll(ConditionalExpr.class).size()
                + unidade.findAll(SwitchEntry.class).stream()
                        .mapToInt(entrada -> entrada.getLabels().size())
                        .sum()
                + (int) unidade.findAll(BinaryExpr.class).stream()
                        .filter(this::ehOperadorLogico)
                        .count();

        int unidadesDeCodigo = unidade.findAll(MethodDeclaration.class).size()
                + unidade.findAll(ConstructorDeclaration.class).size();
        int componentes = Math.max(unidadesDeCodigo, 1);

        int complexidade = decisoes + componentes;
        String detalhe = "%d ponto(s) de decisao em %d metodo(s)/construtor(es) (M = decisoes + P)."
                .formatted(decisoes, componentes);
        return new MetricaCalculada(complexidade, String.valueOf(complexidade), detalhe);
    }

    private boolean ehOperadorLogico(BinaryExpr expressao) {
        return expressao.getOperator() == BinaryExpr.Operator.AND
                || expressao.getOperator() == BinaryExpr.Operator.OR;
    }
}
