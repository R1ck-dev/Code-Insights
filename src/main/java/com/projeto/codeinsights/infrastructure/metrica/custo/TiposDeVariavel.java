package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

/**
 * Tabela de simbolos leve: nome de variavel -> tipo concreto. Substitui o
 * resolvedor de simbolos completo do JavaParser (que exigiria o classpath do
 * aluno) por uma varredura sintatica das declaracoes da unidade de compilacao.
 * <p>
 * Sem isso o motor nao consegue saber que {@code lista.contains(x)} custa O(n)
 * mas {@code conjunto.contains(x)} custa O(1) - a distincao que revela a troca
 * de laco aninhado por tabela hash. Ignora sombreamento de escopo: em codigo de
 * exercicio, dois locais com o mesmo nome e tipos diferentes sao raros.
 */
public final class TiposDeVariavel {

    /** Tipos cujo tamanho cresce com a entrada; usados para medir aninhamento de estruturas. */
    private static final Set<String> COLECOES = Set.of(
            "Collection", "List", "ArrayList", "LinkedList", "Vector", "Stack",
            "Map", "HashMap", "TreeMap", "LinkedHashMap", "Hashtable",
            "Set", "HashSet", "TreeSet", "LinkedHashSet",
            "Queue", "Deque", "ArrayDeque", "PriorityQueue",
            "StringBuilder", "StringBuffer");

    /** Tipos declarados como interface: o motor assume a implementacao mais comum e baixa a confianca. */
    private static final Set<String> INTERFACES = Set.of(
            "Collection", "List", "Map", "Set", "Queue", "Deque");

    private final Map<String, String> tipoConcreto = new HashMap<>();
    private final Map<String, Integer> profundidadeDeColecao = new HashMap<>();

    public static TiposDeVariavel de(CompilationUnit unidade) {
        TiposDeVariavel tipos = new TiposDeVariavel();
        unidade.findAll(Parameter.class)
                .forEach(parametro -> tipos.registrar(parametro.getNameAsString(), parametro.getType(), null));
        unidade.findAll(VariableDeclarator.class)
                .forEach(variavel -> tipos.registrar(variavel.getNameAsString(), variavel.getType(),
                        variavel.getInitializer().orElse(null)));
        return tipos;
    }

    private void registrar(String nome, Type declarado, Expression inicializador) {
        String concreto = inicializador instanceof ObjectCreationExpr criacao
                ? criacao.getType().getNameAsString()
                : nomeSimples(declarado);
        tipoConcreto.put(nome, concreto);
        profundidadeDeColecao.put(nome, profundidade(declarado));
    }

    /** Tipo concreto da variavel, ou {@code null} se ela nao foi declarada nesta unidade. */
    public String tipoDe(String nome) {
        return tipoConcreto.get(nome);
    }

    /** {@code true} quando o tipo so e conhecido pela interface (ex.: {@code List} recebido por parametro). */
    public boolean ehApenasInterface(String nome) {
        return INTERFACES.contains(tipoConcreto.get(nome));
    }

    /**
     * Quantas dimensoes proporcionais a entrada o tipo declarado carrega:
     * {@code int[][]} e {@code List<List<Integer>>} -> 2, {@code Map<Integer, List<Integer>>} -> 2,
     * {@code List<Integer>} -> 1, {@code int} -> 0.
     */
    public int profundidadeDe(String nome) {
        return profundidadeDeColecao.getOrDefault(nome, 0);
    }

    private static int profundidade(Type tipo) {
        if (tipo instanceof ArrayType arranjo) {
            return 1 + profundidade(arranjo.getComponentType());
        }
        if (!(tipo instanceof ClassOrInterfaceType classe) || !COLECOES.contains(classe.getNameAsString())) {
            return 0;
        }
        int interna = classe.getTypeArguments().stream()
                .flatMap(argumentos -> argumentos.stream())
                .mapToInt(TiposDeVariavel::profundidade)
                .max()
                .orElse(0);
        return 1 + interna;
    }

    private static String nomeSimples(Type tipo) {
        if (tipo instanceof ArrayType arranjo) {
            return nomeSimples(arranjo.getComponentType()) + "[]";
        }
        if (tipo instanceof ClassOrInterfaceType classe) {
            return classe.getNameAsString();
        }
        return tipo.asString();
    }
}
