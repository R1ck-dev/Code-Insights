package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

/**
 * Custo assintotico das chamadas da biblioteca padrao do Java.
 * <p>
 * Sem esta tabela, {@code Arrays.sort(v)} custaria O(1) e {@code lista.contains(x)}
 * dentro de um laco pareceria O(n). E justamente aqui que mora o sinal pedagogico
 * central da pesquisa: o aluno que troca o laco aninhado por um {@code HashSet}
 * so aparece na metrica se o motor souber que {@code HashSet.contains} e O(1) e
 * {@code List.contains} e O(n).
 * <p>
 * O {@code n} de cada entrada e o tamanho da estrutura receptora, que o motor
 * aproxima pelo tamanho da entrada (nao ha analise de fluxo de dados que ligue
 * as duas coisas).
 */
public final class TabelaDeCustoApi {

    private static final Set<String> CLASSES_TRIVIAIS = Set.of(
            "Math", "Objects", "Integer", "Long", "Double", "Float", "Short", "Byte",
            "Boolean", "Character", "Scanner", "Random", "Optional", "Thread");

    private static final Map<String, Custo> ARRAYS = Map.of(
            "sort", Custo.N_LOG_N,
            "parallelSort", Custo.N_LOG_N,
            "binarySearch", Custo.LOGARITMICO,
            "fill", Custo.LINEAR,
            "copyOf", Custo.LINEAR,
            "copyOfRange", Custo.LINEAR,
            "equals", Custo.LINEAR,
            "toString", Custo.LINEAR,
            "asList", Custo.LINEAR,
            "stream", Custo.LINEAR);

    private static final Map<String, Custo> COLLECTIONS = Map.of(
            "sort", Custo.N_LOG_N,
            "binarySearch", Custo.LOGARITMICO,
            "reverse", Custo.LINEAR,
            "shuffle", Custo.LINEAR,
            "max", Custo.LINEAR,
            "min", Custo.LINEAR,
            "frequency", Custo.LINEAR,
            "fill", Custo.LINEAR,
            "swap", Custo.CONSTANTE,
            "emptyList", Custo.CONSTANTE);

    /** Operacoes de Stream, reconhecidas pelo nome (o escopo e outra chamada encadeada). */
    private static final Map<String, Custo> STREAM = Map.ofEntries(
            Map.entry("sorted", Custo.N_LOG_N),
            Map.entry("filter", Custo.LINEAR),
            Map.entry("map", Custo.LINEAR),
            Map.entry("mapToInt", Custo.LINEAR),
            Map.entry("mapToObj", Custo.LINEAR),
            Map.entry("boxed", Custo.LINEAR),
            Map.entry("distinct", Custo.LINEAR),
            Map.entry("collect", Custo.LINEAR),
            Map.entry("forEach", Custo.LINEAR),
            Map.entry("anyMatch", Custo.LINEAR),
            Map.entry("allMatch", Custo.LINEAR),
            Map.entry("noneMatch", Custo.LINEAR),
            Map.entry("count", Custo.LINEAR),
            Map.entry("sum", Custo.LINEAR),
            Map.entry("toArray", Custo.LINEAR),
            Map.entry("reduce", Custo.LINEAR));

    private static final Map<String, Custo> LISTA_INDEXADA = Map.ofEntries(
            Map.entry("get", Custo.CONSTANTE),
            Map.entry("set", Custo.CONSTANTE),
            Map.entry("size", Custo.CONSTANTE),
            Map.entry("isEmpty", Custo.CONSTANTE),
            Map.entry("iterator", Custo.CONSTANTE),
            Map.entry("contains", Custo.LINEAR),
            Map.entry("indexOf", Custo.LINEAR),
            Map.entry("lastIndexOf", Custo.LINEAR),
            Map.entry("remove", Custo.LINEAR),
            Map.entry("removeIf", Custo.LINEAR),
            Map.entry("addAll", Custo.LINEAR),
            Map.entry("containsAll", Custo.LINEAR),
            Map.entry("removeAll", Custo.LINEAR),
            Map.entry("retainAll", Custo.LINEAR),
            Map.entry("clear", Custo.LINEAR),
            Map.entry("toArray", Custo.LINEAR),
            Map.entry("sort", Custo.N_LOG_N));

    private static final Map<String, Custo> LISTA_ENCADEADA = Map.ofEntries(
            Map.entry("addFirst", Custo.CONSTANTE),
            Map.entry("addLast", Custo.CONSTANTE),
            Map.entry("removeFirst", Custo.CONSTANTE),
            Map.entry("removeLast", Custo.CONSTANTE),
            Map.entry("peek", Custo.CONSTANTE),
            Map.entry("poll", Custo.CONSTANTE),
            Map.entry("push", Custo.CONSTANTE),
            Map.entry("pop", Custo.CONSTANTE),
            Map.entry("offer", Custo.CONSTANTE),
            Map.entry("size", Custo.CONSTANTE),
            Map.entry("isEmpty", Custo.CONSTANTE),
            Map.entry("get", Custo.LINEAR),
            Map.entry("set", Custo.LINEAR),
            Map.entry("contains", Custo.LINEAR),
            Map.entry("indexOf", Custo.LINEAR),
            Map.entry("remove", Custo.LINEAR));

    private static final Map<String, Custo> TABELA_HASH = Map.ofEntries(
            Map.entry("get", Custo.CONSTANTE),
            Map.entry("put", Custo.CONSTANTE),
            Map.entry("add", Custo.CONSTANTE),
            Map.entry("remove", Custo.CONSTANTE),
            Map.entry("contains", Custo.CONSTANTE),
            Map.entry("containsKey", Custo.CONSTANTE),
            Map.entry("getOrDefault", Custo.CONSTANTE),
            Map.entry("putIfAbsent", Custo.CONSTANTE),
            Map.entry("computeIfAbsent", Custo.CONSTANTE),
            Map.entry("merge", Custo.CONSTANTE),
            Map.entry("size", Custo.CONSTANTE),
            Map.entry("isEmpty", Custo.CONSTANTE),
            Map.entry("keySet", Custo.CONSTANTE),
            Map.entry("values", Custo.CONSTANTE),
            Map.entry("entrySet", Custo.CONSTANTE),
            Map.entry("containsValue", Custo.LINEAR),
            Map.entry("clear", Custo.LINEAR),
            Map.entry("putAll", Custo.LINEAR));

    private static final Map<String, Custo> ARVORE_BALANCEADA = Map.ofEntries(
            Map.entry("get", Custo.LOGARITMICO),
            Map.entry("put", Custo.LOGARITMICO),
            Map.entry("add", Custo.LOGARITMICO),
            Map.entry("remove", Custo.LOGARITMICO),
            Map.entry("contains", Custo.LOGARITMICO),
            Map.entry("containsKey", Custo.LOGARITMICO),
            Map.entry("floorKey", Custo.LOGARITMICO),
            Map.entry("ceilingKey", Custo.LOGARITMICO),
            Map.entry("higher", Custo.LOGARITMICO),
            Map.entry("lower", Custo.LOGARITMICO),
            Map.entry("first", Custo.LOGARITMICO),
            Map.entry("last", Custo.LOGARITMICO),
            Map.entry("firstKey", Custo.LOGARITMICO),
            Map.entry("lastKey", Custo.LOGARITMICO),
            Map.entry("size", Custo.CONSTANTE),
            Map.entry("isEmpty", Custo.CONSTANTE));

    private static final Map<String, Custo> FILA_DE_PRIORIDADE = Map.of(
            "add", Custo.LOGARITMICO,
            "offer", Custo.LOGARITMICO,
            "poll", Custo.LOGARITMICO,
            "peek", Custo.CONSTANTE,
            "size", Custo.CONSTANTE,
            "isEmpty", Custo.CONSTANTE,
            "remove", Custo.LINEAR,
            "contains", Custo.LINEAR);

    private static final Map<String, Custo> DEQUE = Map.ofEntries(
            Map.entry("push", Custo.CONSTANTE),
            Map.entry("pop", Custo.CONSTANTE),
            Map.entry("peek", Custo.CONSTANTE),
            Map.entry("poll", Custo.CONSTANTE),
            Map.entry("offer", Custo.CONSTANTE),
            Map.entry("add", Custo.CONSTANTE),
            Map.entry("addFirst", Custo.CONSTANTE),
            Map.entry("addLast", Custo.CONSTANTE),
            Map.entry("pollFirst", Custo.CONSTANTE),
            Map.entry("pollLast", Custo.CONSTANTE),
            Map.entry("removeFirst", Custo.CONSTANTE),
            Map.entry("removeLast", Custo.CONSTANTE),
            Map.entry("size", Custo.CONSTANTE),
            Map.entry("isEmpty", Custo.CONSTANTE),
            Map.entry("contains", Custo.LINEAR),
            Map.entry("search", Custo.LINEAR));

    private static final Map<String, Custo> TEXTO = Map.ofEntries(
            Map.entry("length", Custo.CONSTANTE),
            Map.entry("charAt", Custo.CONSTANTE),
            Map.entry("isEmpty", Custo.CONSTANTE),
            Map.entry("equals", Custo.LINEAR),
            Map.entry("equalsIgnoreCase", Custo.LINEAR),
            Map.entry("compareTo", Custo.LINEAR),
            Map.entry("contains", Custo.LINEAR),
            Map.entry("indexOf", Custo.LINEAR),
            Map.entry("substring", Custo.LINEAR),
            Map.entry("toCharArray", Custo.LINEAR),
            Map.entry("split", Custo.LINEAR),
            Map.entry("replace", Custo.LINEAR),
            Map.entry("trim", Custo.LINEAR),
            Map.entry("strip", Custo.LINEAR),
            Map.entry("toLowerCase", Custo.LINEAR),
            Map.entry("toUpperCase", Custo.LINEAR),
            Map.entry("concat", Custo.LINEAR),
            Map.entry("matches", Custo.LINEAR),
            Map.entry("startsWith", Custo.LINEAR),
            Map.entry("endsWith", Custo.LINEAR),
            Map.entry("chars", Custo.LINEAR),
            Map.entry("repeat", Custo.LINEAR));

    private static final Map<String, Custo> CONSTRUTOR_DE_TEXTO = Map.of(
            "append", Custo.CONSTANTE,
            "charAt", Custo.CONSTANTE,
            "length", Custo.CONSTANTE,
            "setCharAt", Custo.CONSTANTE,
            "setLength", Custo.CONSTANTE,
            "toString", Custo.LINEAR,
            "reverse", Custo.LINEAR,
            "insert", Custo.LINEAR,
            "delete", Custo.LINEAR,
            "deleteCharAt", Custo.LINEAR);

    private static final Map<String, Map<String, Custo>> POR_TIPO = Map.ofEntries(
            Map.entry("List", LISTA_INDEXADA),
            Map.entry("ArrayList", LISTA_INDEXADA),
            Map.entry("Vector", LISTA_INDEXADA),
            Map.entry("LinkedList", LISTA_ENCADEADA),
            Map.entry("Map", TABELA_HASH),
            Map.entry("HashMap", TABELA_HASH),
            Map.entry("LinkedHashMap", TABELA_HASH),
            Map.entry("Hashtable", TABELA_HASH),
            Map.entry("Set", TABELA_HASH),
            Map.entry("HashSet", TABELA_HASH),
            Map.entry("LinkedHashSet", TABELA_HASH),
            Map.entry("TreeMap", ARVORE_BALANCEADA),
            Map.entry("TreeSet", ARVORE_BALANCEADA),
            Map.entry("PriorityQueue", FILA_DE_PRIORIDADE),
            Map.entry("ArrayDeque", DEQUE),
            Map.entry("Deque", DEQUE),
            Map.entry("Queue", DEQUE),
            Map.entry("Stack", DEQUE),
            Map.entry("String", TEXTO),
            Map.entry("StringBuilder", CONSTRUTOR_DE_TEXTO),
            Map.entry("StringBuffer", CONSTRUTOR_DE_TEXTO));

    private TabelaDeCustoApi() {
    }

    /**
     * Custo da chamada, ou vazio quando ela nao e reconhecida como API da biblioteca
     * padrao (o avaliador entao decide o que fazer com uma chamada desconhecida).
     */
    public static Optional<CustoAvaliado> custo(MethodCallExpr chamada, TiposDeVariavel tipos) {
        Expression escopo = chamada.getScope().orElse(null);
        if (escopo == null) {
            return Optional.empty();
        }
        if (escopo instanceof FieldAccessExpr || escopo instanceof MethodCallExpr) {
            return custoDeEscopoDerivado(chamada, escopo);
        }
        if (escopo instanceof StringLiteralExpr) {
            return busca(TEXTO, chamada).map(CustoAvaliado::exato);
        }
        if (escopo instanceof NameExpr referencia) {
            return custoDeNome(chamada, referencia.getNameAsString(), tipos);
        }
        return Optional.empty();
    }

    /** {@code System.out.println(...)} ou uma chamada encadeada de Stream. */
    private static Optional<CustoAvaliado> custoDeEscopoDerivado(MethodCallExpr chamada, Expression escopo) {
        if (escopo instanceof FieldAccessExpr campo && campo.getNameAsString().equals("out")) {
            return Optional.of(CustoAvaliado.exato(Custo.CONSTANTE));
        }
        return Optional.ofNullable(STREAM.get(chamada.getNameAsString())).map(CustoAvaliado::exato);
    }

    private static Optional<CustoAvaliado> custoDeNome(MethodCallExpr chamada, String nome, TiposDeVariavel tipos) {
        if (nome.equals("Arrays")) {
            return Optional.of(CustoAvaliado.exato(ARRAYS.getOrDefault(chamada.getNameAsString(), Custo.LINEAR)));
        }
        if (nome.equals("Collections")) {
            return Optional.of(CustoAvaliado.exato(COLLECTIONS.getOrDefault(chamada.getNameAsString(), Custo.LINEAR)));
        }
        if (nome.equals("System")) {
            Custo custo = chamada.getNameAsString().equals("arraycopy") ? Custo.LINEAR : Custo.CONSTANTE;
            return Optional.of(CustoAvaliado.exato(custo));
        }
        if (CLASSES_TRIVIAIS.contains(nome)) {
            return Optional.of(CustoAvaliado.exato(Custo.CONSTANTE));
        }
        return custoDeInstancia(chamada, nome, tipos);
    }

    private static Optional<CustoAvaliado> custoDeInstancia(MethodCallExpr chamada, String receptor,
            TiposDeVariavel tipos) {
        String tipo = tipos.tipoDe(receptor);
        if (tipo == null) {
            return Optional.empty();
        }
        if (CLASSES_TRIVIAIS.contains(tipo)) {
            return Optional.of(CustoAvaliado.exato(Custo.CONSTANTE));
        }
        Map<String, Custo> tabela = POR_TIPO.get(tipo);
        if (tabela == null) {
            return Optional.empty();
        }
        Custo custo = ajustarPorAridade(tabela, chamada)
                .orElseGet(() -> busca(tabela, chamada).orElse(Custo.CONSTANTE));
        if (!tipos.ehApenasInterface(receptor)) {
            return Optional.of(CustoAvaliado.exato(custo));
        }
        return Optional.of(CustoAvaliado.estimado(custo,
                "tipo de `%s` conhecido so pela interface %s; assumida a implementacao usual"
                        .formatted(receptor, tipo)));
    }

    /** {@code lista.add(e)} e O(1), mas {@code lista.add(i, e)} desloca a cauda e custa O(n). */
    private static Optional<Custo> ajustarPorAridade(Map<String, Custo> tabela, MethodCallExpr chamada) {
        if (tabela == LISTA_INDEXADA && chamada.getNameAsString().equals("add")) {
            return Optional.of(chamada.getArguments().size() >= 2 ? Custo.LINEAR : Custo.CONSTANTE);
        }
        return Optional.empty();
    }

    private static Optional<Custo> busca(Map<String, Custo> tabela, MethodCallExpr chamada) {
        return Optional.ofNullable(tabela.get(chamada.getNameAsString()));
    }
}
