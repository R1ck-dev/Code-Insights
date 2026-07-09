package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.infrastructure.metrica.custo.AnalisadorDeRecursao.Recursao;

/**
 * Avalia a memoria auxiliar de uma solucao: o maior entre o que ela <b>aloca</b> e a
 * <b>profundidade da pilha</b> de recursao.
 * <p>
 * A profundidade da pilha vem da mesma recorrencia que o motor de tempo extrai, o que
 * corrige o erro mais visivel da versao anterior ("qualquer recursao -> O(n)"): a busca
 * binaria recursiva desce {@code log n} niveis, nao {@code n}. E o aninhamento de
 * colecoes vem do tipo declarado, entao {@code List<List<Integer>>} custa {@code O(n^2)}
 * de espaco, e nao {@code O(n)}.
 */
public final class AvaliadorDeEspaco {

    private static final Set<String> COLECOES = Set.of(
            "ArrayList", "LinkedList", "HashMap", "TreeMap", "LinkedHashMap",
            "HashSet", "TreeSet", "LinkedHashSet", "ArrayDeque", "PriorityQueue",
            "Stack", "Vector", "StringBuilder", "StringBuffer");

    /** Chamadas que devolvem uma copia proporcional a entrada. */
    private static final Set<String> ALOCADORAS_LINEARES = Set.of(
            "toCharArray", "split", "copyOf", "copyOfRange", "clone", "toArray");

    private AvaliadorDeEspaco() {
    }

    public static CustoAvaliado doPrograma(CompilationUnit unidade) {
        TiposDeVariavel tipos = TiposDeVariavel.de(unidade);
        return maiorAlocacao(unidade, tipos).mais(profundidadeDaPilha(unidade));
    }

    private static CustoAvaliado maiorAlocacao(CompilationUnit unidade, TiposDeVariavel tipos) {
        CustoAvaliado maior = CustoAvaliado.exato(Custo.CONSTANTE);
        for (ArrayCreationExpr criacao : unidade.findAll(ArrayCreationExpr.class)) {
            maior = maior.mais(CustoAvaliado.exato(Custo.poliLog(dimensoesVariaveis(criacao), 0)));
        }
        for (ObjectCreationExpr criacao : unidade.findAll(ObjectCreationExpr.class)) {
            if (COLECOES.contains(criacao.getType().getNameAsString())) {
                maior = maior.mais(custoDeColecao(criacao, tipos));
            }
        }
        for (MethodCallExpr chamada : unidade.findAll(MethodCallExpr.class)) {
            if (ALOCADORAS_LINEARES.contains(chamada.getNameAsString())) {
                maior = maior.mais(CustoAvaliado.exato(Custo.LINEAR));
            }
        }
        for (AssignExpr atribuicao : unidade.findAll(AssignExpr.class)) {
            if (ehConcatenacaoDeTexto(atribuicao, tipos)) {
                maior = maior.mais(CustoAvaliado.exato(Custo.LINEAR)
                        .comNota("a String acumulada cresce com a entrada"));
            }
        }
        return maior;
    }

    private static boolean ehConcatenacaoDeTexto(AssignExpr atribuicao, TiposDeVariavel tipos) {
        return atribuicao.getOperator() == AssignExpr.Operator.PLUS
                && atribuicao.getTarget().isNameExpr()
                && "String".equals(tipos.tipoDe(atribuicao.getTarget().asNameExpr().getNameAsString()));
    }

    /** {@code new int[n]} -> grau 1; {@code new int[n][n]} -> grau 2; {@code new int[26]} -> grau 0 (constante). */
    private static int dimensoesVariaveis(ArrayCreationExpr criacao) {
        return (int) criacao.getLevels().stream()
                .filter(nivel -> nivel.getDimension().map(AvaliadorDeCusto::dependeDaEntrada).orElse(false))
                .count();
    }

    private static CustoAvaliado custoDeColecao(ObjectCreationExpr criacao, TiposDeVariavel tipos) {
        int aninhamento = Math.max(1, profundidadeDeclarada(criacao, tipos));
        return CustoAvaliado.estimado(Custo.poliLog(aninhamento, 0),
                "colecao dinamica assumida proporcional a entrada (aninhamento %d pelo tipo declarado)"
                        .formatted(aninhamento));
    }

    private static int profundidadeDeclarada(ObjectCreationExpr criacao, TiposDeVariavel tipos) {
        Node pai = criacao.getParentNode().orElse(null);
        if (pai instanceof VariableDeclarator declaracao) {
            return tipos.profundidadeDe(declaracao.getNameAsString());
        }
        if (pai instanceof AssignExpr atribuicao && atribuicao.getTarget().isNameExpr()) {
            return tipos.profundidadeDe(atribuicao.getTarget().asNameExpr().getNameAsString());
        }
        return 1;
    }

    /** A recorrencia diz quantos niveis a recursao desce - e, portanto, quantos quadros a pilha guarda. */
    private static CustoAvaliado profundidadeDaPilha(CompilationUnit unidade) {
        CustoAvaliado maior = CustoAvaliado.exato(Custo.CONSTANTE);
        for (MethodDeclaration metodo : unidade.findAll(MethodDeclaration.class)) {
            if (!AnalisadorDeRecursao.ehRecursivo(metodo)) {
                continue;
            }
            maior = maior.mais(custoDaPilha(AnalisadorDeRecursao.analisar(metodo)));
        }
        if (!GrafoDeChamadas.temCicloIndireto(unidade)) {
            return maior;
        }
        return maior.mais(CustoAvaliado.estimado(Custo.LINEAR,
                "recursao mutua: profundidade da pilha nao derivavel de um metodo so; assumidos n niveis"));
    }

    private static CustoAvaliado custoDaPilha(Recursao recursao) {
        CustoAvaliado profundidade = switch (recursao.reducao()) {
            case DIVISIVA -> CustoAvaliado.exato(Custo.LOGARITMICO)
                    .comNota("pilha de recursao: a divisao e conquista desce log n niveis");
            case INDETERMINADA -> CustoAvaliado.estimado(Custo.LINEAR,
                    "profundidade da pilha nao determinada; assumidos n niveis");
            default -> CustoAvaliado.exato(Custo.LINEAR).comNota("pilha de recursao: n niveis");
        };
        return recursao.temSuposicao() ? profundidade.rebaixado(NivelConfianca.MEDIA) : profundidade;
    }
}
