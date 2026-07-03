package com.projeto.codeinsights.infrastructure.metrica;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.projeto.codeinsights.domain.knowledge.enums.ClasseComplexidade;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;

/**
 * Estimativa heuristica da complexidade de espaco (memoria auxiliar) por analise
 * estatica da AST. Como no Big O de tempo, e uma aproximacao conservadora, nao um
 * valor provado. O grau estimado e o maior entre tres sinais:
 * <ul>
 *   <li>arrays dimensionados pela entrada: {@code new int[n]} -> grau 1,
 *       {@code new int[n][n]} -> grau 2 (dimensoes com tamanho literal constante
 *       nao contam, pois sao O(1));</li>
 *   <li>estruturas de dados dinamicas (List, Map, Set, Deque, StringBuilder...) ->
 *       grau 1 (assumidas proporcionais a entrada);</li>
 *   <li>recursao -> grau 1 (profundidade da pilha de chamadas).</li>
 * </ul>
 * grau 0 -> O(1), 1 -> O(n), 2 -> O(n^2), 3+ -> O(n^3).
 */
@Component
class EspacoAnalisador implements AnalisadorMetricaJava {

    private static final Set<String> COLECOES_DINAMICAS = Set.of(
            "ArrayList", "LinkedList", "HashMap", "TreeMap", "LinkedHashMap",
            "HashSet", "TreeSet", "LinkedHashSet", "ArrayDeque", "PriorityQueue",
            "Stack", "Vector", "StringBuilder", "StringBuffer");

    @Override
    public TipoMetrica tipo() {
        return TipoMetrica.COMPLEXIDADE_ESPACO;
    }

    @Override
    public MetricaCalculada analisar(CompilationUnit unidade) {
        int dimensaoArrays = maiorDimensaoDeArray(unidade);
        boolean temColecao = temColecaoDinamica(unidade);
        boolean temRecursao = AstUtils.grauDeRecursao(unidade) > 0;

        int grau = Math.max(dimensaoArrays, Math.max(temColecao ? 1 : 0, temRecursao ? 1 : 0));
        ClasseComplexidade classe = classePorGrau(grau);

        String detalhe = "arrays sobre a entrada = grau %d; colecao dinamica = %s; recursao = %s -> estimativa %s."
                .formatted(dimensaoArrays, temColecao ? "sim" : "nao",
                        temRecursao ? "sim" : "nao", classe.getRotulo());
        return new MetricaCalculada(classe.getOrdem(), classe.getRotulo(), detalhe);
    }

    private int maiorDimensaoDeArray(CompilationUnit unidade) {
        int maior = 0;
        for (ArrayCreationExpr criacao : unidade.findAll(ArrayCreationExpr.class)) {
            int dimensoesVariaveis = (int) criacao.getLevels().stream()
                    .filter(nivel -> nivel.getDimension()
                            .map(tamanho -> !tamanho.isIntegerLiteralExpr() && !tamanho.isLongLiteralExpr())
                            .orElse(false))
                    .count();
            maior = Math.max(maior, dimensoesVariaveis);
        }
        return maior;
    }

    private boolean temColecaoDinamica(CompilationUnit unidade) {
        return unidade.findAll(ObjectCreationExpr.class).stream()
                .anyMatch(criacao -> COLECOES_DINAMICAS.contains(criacao.getType().getNameAsString()));
    }

    private ClasseComplexidade classePorGrau(int grau) {
        return switch (grau) {
            case 0 -> ClasseComplexidade.O_1;
            case 1 -> ClasseComplexidade.O_N;
            case 2 -> ClasseComplexidade.O_N2;
            default -> ClasseComplexidade.O_N3;
        };
    }
}
