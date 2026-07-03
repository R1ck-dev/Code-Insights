package com.projeto.codeinsights.infrastructure.metrica;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.projeto.codeinsights.domain.knowledge.enums.ClasseComplexidade;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;

/**
 * Estimativa heuristica da complexidade de tempo (Big O) por analise estatica da
 * AST. Inferir o Big O exato de codigo arbitrario e indecidivel; esta metrica e
 * uma aproximacao conservadora e transparente, pensada para mapear a curva de
 * amadurecimento algoritmico (forca bruta -> estruturas refinadas), nao um valor
 * formalmente provado.
 * <p>
 * A classe estimada e a <b>pior</b> entre dois sinais independentes:
 * <ul>
 *   <li><b>lacos</b>: a cadeia de lacos aninhados mais custosa (por caminho da
 *       AST, nao lacos soltos somados). Cada laco na cadeia multiplica por n, ou
 *       por log n quando seu indice avanca de forma multiplicativa (i*=2, i/=2,
 *       i&lt;&lt;=1). Assim n * n -> O(n^2), n * log n -> O(n log n), log n
 *       isolado -> O(log n);</li>
 *   <li><b>recursao</b>: recursao linear -> O(n); recursao com 2+ auto-chamadas
 *       (arvore de recursao) -> O(2^n).</li>
 * </ul>
 * Limitacao conhecida: recursao e lacos sao combinados por max, nao por produto,
 * podendo subestimar casos como recursao que itera sobre a entrada.
 */
@Component
class BigOTempoAnalisador implements AnalisadorMetricaJava {

    /** Cadeia de aninhamento de lacos dominante: {@code linear} fatores n, {@code log} fatores log n. */
    private record Cadeia(int linear, int log) {

        Cadeia mais(Cadeia outra) {
            if (this.linear != outra.linear) {
                return this.linear > outra.linear ? this : outra;
            }
            return this.log >= outra.log ? this : outra;
        }
    }

    @Override
    public TipoMetrica tipo() {
        return TipoMetrica.BIG_O_TEMPO;
    }

    @Override
    public MetricaCalculada analisar(CompilationUnit unidade) {
        Cadeia cadeia = cadeiaDominante(unidade);
        int grauRecursao = AstUtils.grauDeRecursao(unidade);

        ClasseComplexidade porLacos = classePorCadeia(cadeia);
        ClasseComplexidade porRecursao = classePorRecursao(grauRecursao);
        ClasseComplexidade classe = porLacos.maisComplexa(porRecursao);

        String detalhe = "cadeia de lacos dominante = %d fator(es) n x %d fator(es) log n; recursao = %s -> estimativa %s."
                .formatted(cadeia.linear(), cadeia.log(), descreveRecursao(grauRecursao), classe.getRotulo());
        return new MetricaCalculada(classe.getOrdem(), classe.getRotulo(), detalhe);
    }

    /** Cadeia de lacos aninhados mais custosa entre todos os caminhos abaixo de {@code no}. */
    private Cadeia cadeiaDominante(Node no) {
        Cadeia melhorFilho = new Cadeia(0, 0);
        for (Node filho : no.getChildNodes()) {
            melhorFilho = melhorFilho.mais(cadeiaDominante(filho));
        }
        if (AstUtils.ehLaco(no)) {
            return ehLacoLogaritmico(no)
                    ? new Cadeia(melhorFilho.linear(), melhorFilho.log() + 1)
                    : new Cadeia(melhorFilho.linear() + 1, melhorFilho.log());
        }
        return melhorFilho;
    }

    private ClasseComplexidade classePorCadeia(Cadeia cadeia) {
        return switch (cadeia.linear()) {
            case 0 -> cadeia.log() > 0 ? ClasseComplexidade.O_LOG_N : ClasseComplexidade.O_1;
            case 1 -> cadeia.log() > 0 ? ClasseComplexidade.O_N_LOG_N : ClasseComplexidade.O_N;
            case 2 -> ClasseComplexidade.O_N2;
            default -> ClasseComplexidade.O_N3;
        };
    }

    private ClasseComplexidade classePorRecursao(int grauRecursao) {
        return switch (grauRecursao) {
            case 2 -> ClasseComplexidade.O_EXPONENCIAL;
            case 1 -> ClasseComplexidade.O_N;
            default -> ClasseComplexidade.O_1;
        };
    }

    private String descreveRecursao(int grauRecursao) {
        return switch (grauRecursao) {
            case 2 -> "exponencial (2+ auto-chamadas)";
            case 1 -> "linear";
            default -> "ausente";
        };
    }

    /**
     * O laco e logaritmico quando a variavel que <b>governa sua terminacao</b> (a que
     * aparece na condicao) e atualizada de forma multiplicativa (i*=2, i/=2, i&gt;&gt;=1,
     * i=i/2). Acumuladores no corpo (fat*=i, hash*=31) nao contam: nao controlam a parada.
     */
    private boolean ehLacoLogaritmico(Node laco) {
        Set<String> variaveisDeTerminacao = variaveisDaCondicao(laco);
        if (variaveisDeTerminacao.isEmpty()) {
            return false;
        }
        return laco.findAll(AssignExpr.class).stream()
                .filter(atribuicao -> AstUtils.lacoMaisProximo(atribuicao) == laco)
                .filter(this::ehAtualizacaoMultiplicativa)
                .anyMatch(atribuicao -> variaveisDeTerminacao.contains(nomeDoAlvo(atribuicao)));
    }

    private Set<String> variaveisDaCondicao(Node laco) {
        Expression condicao = null;
        if (laco instanceof ForStmt paraLaco) {
            condicao = paraLaco.getCompare().orElse(null);
        } else if (laco instanceof WhileStmt enquantoLaco) {
            condicao = enquantoLaco.getCondition();
        } else if (laco instanceof DoStmt facaLaco) {
            condicao = facaLaco.getCondition();
        }
        if (condicao == null) {
            return Set.of();
        }
        return condicao.findAll(NameExpr.class).stream()
                .map(NameExpr::getNameAsString)
                .collect(Collectors.toSet());
    }

    private String nomeDoAlvo(AssignExpr atribuicao) {
        return atribuicao.getTarget().isNameExpr()
                ? atribuicao.getTarget().asNameExpr().getNameAsString()
                : null;
    }

    private boolean ehAtualizacaoMultiplicativa(AssignExpr atribuicao) {
        if (ehOperadorCompostoMultiplicativo(atribuicao.getOperator())) {
            return true;
        }
        if (atribuicao.getOperator() == AssignExpr.Operator.ASSIGN && atribuicao.getValue().isBinaryExpr()) {
            BinaryExpr binaria = atribuicao.getValue().asBinaryExpr();
            return ehOperadorBinarioMultiplicativo(binaria.getOperator())
                    && referenciaAlvo(atribuicao.getTarget(), atribuicao.getValue());
        }
        return false;
    }

    private boolean ehOperadorCompostoMultiplicativo(AssignExpr.Operator operador) {
        return operador == AssignExpr.Operator.MULTIPLY || operador == AssignExpr.Operator.DIVIDE
                || operador == AssignExpr.Operator.LEFT_SHIFT
                || operador == AssignExpr.Operator.SIGNED_RIGHT_SHIFT
                || operador == AssignExpr.Operator.UNSIGNED_RIGHT_SHIFT;
    }

    private boolean ehOperadorBinarioMultiplicativo(BinaryExpr.Operator operador) {
        return operador == BinaryExpr.Operator.MULTIPLY || operador == BinaryExpr.Operator.DIVIDE
                || operador == BinaryExpr.Operator.LEFT_SHIFT
                || operador == BinaryExpr.Operator.SIGNED_RIGHT_SHIFT
                || operador == BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT;
    }

    /** O valor da atribuicao referencia o proprio alvo (i = i * 2), nao um produto qualquer (t = a * b). */
    private boolean referenciaAlvo(Expression alvo, Expression valor) {
        if (!alvo.isNameExpr()) {
            return false;
        }
        String nome = alvo.asNameExpr().getNameAsString();
        return valor.findAll(NameExpr.class).stream()
                .anyMatch(referencia -> referencia.getNameAsString().equals(nome));
    }
}
