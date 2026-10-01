package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

/**
 * Liga cada chamada de metodo ao metodo da propria unidade que ela invoca, pelo <b>tipo do
 * receptor</b> e nao pela forma do escopo.
 * <p>
 * A versao anterior so reconhecia chamada sem escopo ou com {@code this}. Qualquer outro receptor
 * ({@code ord.bolha(v)}, {@code Ordenacao.bolha(v)}, {@code esq.altura()}) caia em "externa
 * desconhecida, assumida O(1)", e o metodo do aluno sumia do custo — o erro perigoso, o que diz ao
 * aluno que a solucao custa menos do que custa. Pior: como o ponto de entrada e so o {@code main},
 * o mesmo algoritmo mudava de classe conforme o aluno escrevesse ou nao um.
 * <p>
 * O receptor e tipado pela tabela de simbolos leve ({@link TiposDeVariavel}), pelo tipo de retorno
 * dos metodos da unidade e pelos argumentos genericos das colecoes ({@code adj.get(u)} com
 * {@code adj: List<List<Integer>>} e uma {@code List<Integer>}). Receptor de tipo que o motor nao
 * sabe dizer, ou de um tipo que nao e da unidade ({@code List}, {@code String}), nao e resolvido: a
 * chamada segue para a tabela de custo da biblioteca. Resolver pelo nome quando o tipo e
 * desconhecido ligaria {@code Arrays.sort(v)} a um {@code sort(v)} do aluno e inventaria uma
 * recursao que nao existe.
 * <p>
 * A identidade de um metodo e o proprio no da AST, nao {@code nome/aridade}: duas classes com um
 * {@code insere(int)} deixam de colidir. Sobrecargas de mesma aridade na mesma classe continuam
 * colidindo — vence a ultima declarada — e sao uma limitacao conhecida (nota tecnica, §5.3).
 */
public final class ResolvedorDeChamadas {

    private static final Set<String> MAPAS = Set.of("Map", "HashMap", "TreeMap", "LinkedHashMap", "Hashtable");
    private static final Set<String> SEQUENCIAS = Set.of(
            "List", "ArrayList", "LinkedList", "Vector", "Stack", "Queue", "Deque", "ArrayDeque",
            "PriorityQueue", "Set", "HashSet", "TreeSet", "LinkedHashSet");

    /** Metodos de mapa que devolvem o valor. */
    private static final Set<String> DEVOLVEM_VALOR = Set.of(
            "get", "getOrDefault", "put", "putIfAbsent", "remove", "computeIfAbsent", "merge");

    /**
     * Metodos de sequencia que devolvem um elemento. {@code remove} fica de fora de proposito:
     * {@code remove(int)} devolve o elemento, mas {@code remove(Object)} devolve {@code boolean}.
     */
    private static final Set<String> DEVOLVEM_ELEMENTO = Set.of(
            "get", "getFirst", "getLast", "peek", "poll", "pop", "peekFirst", "peekLast", "pollFirst",
            "pollLast", "removeFirst", "removeLast", "element", "first", "last",
            "floor", "ceiling", "higher", "lower");

    private final TiposDeVariavel tipos;
    private final List<MethodDeclaration> metodos;
    private final Map<String, TypeDeclaration<?>> classes = new HashMap<>();

    private ResolvedorDeChamadas(CompilationUnit unidade) {
        this.tipos = TiposDeVariavel.de(unidade);
        this.metodos = unidade.findAll(MethodDeclaration.class);
        unidade.findAll(TypeDeclaration.class).forEach(tipo -> classes.putIfAbsent(tipo.getNameAsString(), tipo));
    }

    public static ResolvedorDeChamadas de(CompilationUnit unidade) {
        return new ResolvedorDeChamadas(unidade);
    }

    /** Conjunto que compara por identidade: dois metodos (ou chamadas) de texto igual continuam distintos. */
    static <T> Set<T> conjuntoPorIdentidade() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    public TiposDeVariavel tipos() {
        return tipos;
    }

    /** Todos os metodos da unidade, na ordem em que aparecem. */
    public List<MethodDeclaration> metodos() {
        return metodos;
    }

    /** O metodo da unidade que a chamada invoca, ou vazio se ela vai para a biblioteca ou nao se sabe. */
    public Optional<MethodDeclaration> alvo(MethodCallExpr chamada) {
        String nome = chamada.getNameAsString();
        int aridade = chamada.getArguments().size();
        Optional<Expression> escopo = chamada.getScope();
        if (escopo.isEmpty() || escopo.get() instanceof ThisExpr) {
            return alvoSemReceptor(chamada, nome, aridade);
        }
        return classeDoReceptor(escopo.get()).flatMap(classe -> metodoDe(classe, nome, aridade));
    }

    /** A chamada e ao proprio {@code metodo} — por {@code this}, sem escopo, pelo nome da classe ou por outro objeto. */
    public boolean ehAutoChamada(MethodCallExpr chamada, MethodDeclaration metodo) {
        return alvo(chamada).filter(alvo -> alvo == metodo).isPresent();
    }

    /**
     * O receptor e outra instancia ({@code esq.altura()}), e nao o mesmo objeto ({@code this}) nem a
     * classe ({@code Main.fib}). E o que distingue percorrer uma estrutura de recursar sobre o
     * argumento.
     */
    public boolean chamaOutraInstancia(MethodCallExpr chamada) {
        return chamada.getScope()
                .filter(escopo -> !(escopo instanceof ThisExpr) && !ehNomeDeClasse(escopo))
                .isPresent();
    }

    /** Metodos da unidade que {@code metodo} chama; a auto-chamada nao conta. */
    public Set<MethodDeclaration> chamadosPor(MethodDeclaration metodo) {
        Set<MethodDeclaration> chamados = conjuntoPorIdentidade();
        metodo.findAll(MethodCallExpr.class)
                .forEach(chamada -> alvo(chamada).filter(alvo -> alvo != metodo).ifPresent(chamados::add));
        return chamados;
    }

    /** Tipo estatico da expressao, ou vazio quando o motor nao sabe dizer. */
    public Optional<Type> tipoDe(Expression expressao) {
        if (expressao instanceof EnclosedExpr entreParenteses) {
            return tipoDe(entreParenteses.getInner());
        }
        if (expressao instanceof CastExpr conversao) {
            return Optional.of(conversao.getType());
        }
        if (expressao instanceof NameExpr nome) {
            return tipos.tipoDeclarado(nome.getNameAsString());
        }
        if (expressao instanceof FieldAccessExpr campo) {
            return tipos.tipoDeclarado(campo.getNameAsString());
        }
        if (expressao instanceof ObjectCreationExpr criacao) {
            return Optional.of(criacao.getType());
        }
        if (expressao instanceof StringLiteralExpr) {
            return Optional.of(new ClassOrInterfaceType(null, "String"));
        }
        if (expressao instanceof ThisExpr) {
            return tipoEnvolvente(expressao).map(tipo -> new ClassOrInterfaceType(null, tipo.getNameAsString()));
        }
        if (expressao instanceof ArrayAccessExpr acesso) {
            return tipoDe(acesso.getName())
                    .filter(ArrayType.class::isInstance)
                    .map(arranjo -> ((ArrayType) arranjo).getComponentType());
        }
        if (expressao instanceof MethodCallExpr chamada) {
            return tipoDoResultado(chamada);
        }
        return Optional.empty();
    }

    private Optional<Type> tipoDoResultado(MethodCallExpr chamada) {
        Optional<MethodDeclaration> alvo = alvo(chamada);
        if (alvo.isPresent()) {
            Type retorno = alvo.get().getType();
            return retorno.isVoidType() ? Optional.empty() : Optional.of(retorno);
        }
        return chamada.getScope()
                .flatMap(this::tipoDe)
                .flatMap(receptor -> elementoDe(receptor, chamada.getNameAsString()));
    }

    /** {@code lista.get(i)} de {@code List<T>} e {@code T}; {@code mapa.get(k)} de {@code Map<K, V>} e {@code V}. */
    private static Optional<Type> elementoDe(Type receptor, String metodo) {
        if (!(receptor instanceof ClassOrInterfaceType colecao)) {
            return Optional.empty();
        }
        List<Type> argumentos = colecao.getTypeArguments().<List<Type>>map(lista -> lista).orElse(List.of());
        String nome = colecao.getNameAsString();
        if (MAPAS.contains(nome) && DEVOLVEM_VALOR.contains(metodo) && argumentos.size() == 2) {
            return Optional.of(argumentos.get(1));
        }
        if (SEQUENCIAS.contains(nome) && DEVOLVEM_ELEMENTO.contains(metodo) && argumentos.size() == 1) {
            return Optional.of(argumentos.get(0));
        }
        return Optional.empty();
    }

    /** Chamada sem escopo ou com {@code this}: a classe que a envolve, depois as de fora, depois as supertipos. */
    private Optional<MethodDeclaration> alvoSemReceptor(Node chamada, String nome, int aridade) {
        Optional<TypeDeclaration<?>> classe = tipoEnvolvente(chamada);
        while (classe.isPresent()) {
            Optional<MethodDeclaration> achado = metodoDe(classe.get(), nome, aridade);
            if (achado.isPresent()) {
                return achado;
            }
            classe = tipoEnvolvente(classe.get());
        }
        // Corpo de classe anonima nao e um TypeDeclaration: ali o metodo so se acha pelo nome.
        return metodos.stream()
                .filter(metodo -> ehMetodo(metodo, nome, aridade))
                .reduce((primeiro, ultimo) -> ultimo);
    }

    private Optional<TypeDeclaration<?>> classeDoReceptor(Expression receptor) {
        if (ehNomeDeClasse(receptor)) {
            return Optional.of(classes.get(receptor.asNameExpr().getNameAsString()));
        }
        return tipoDe(receptor).flatMap(TiposDeVariavel::nomeDe).map(classes::get);
    }

    /** {@code Ordenacao} em {@code Ordenacao.bolha(v)}: um nome que nao e variavel e e classe da unidade. */
    private boolean ehNomeDeClasse(Expression escopo) {
        return escopo instanceof NameExpr nome
                && tipos.tipoDeclarado(nome.getNameAsString()).isEmpty()
                && classes.containsKey(nome.getNameAsString());
    }

    private Optional<MethodDeclaration> metodoDe(TypeDeclaration<?> tipo, String nome, int aridade) {
        return metodoDe(tipo, nome, aridade, conjuntoPorIdentidade());
    }

    private Optional<MethodDeclaration> metodoDe(TypeDeclaration<?> tipo, String nome, int aridade,
            Set<TypeDeclaration<?>> vistos) {
        if (!vistos.add(tipo)) {
            return Optional.empty();
        }
        Optional<MethodDeclaration> proprio = tipo.getMethods().stream()
                .filter(metodo -> ehMetodo(metodo, nome, aridade))
                .reduce((primeiro, ultimo) -> ultimo);
        if (proprio.isPresent() || !(tipo instanceof ClassOrInterfaceDeclaration declaracao)) {
            return proprio;
        }
        return Stream.concat(declaracao.getExtendedTypes().stream(), declaracao.getImplementedTypes().stream())
                .map(supertipo -> classes.get(supertipo.getNameAsString()))
                .filter(Objects::nonNull)
                .map(supertipo -> metodoDe(supertipo, nome, aridade, vistos))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static boolean ehMetodo(MethodDeclaration metodo, String nome, int aridade) {
        return metodo.getNameAsString().equals(nome) && metodo.getParameters().size() == aridade;
    }

    private static Optional<TypeDeclaration<?>> tipoEnvolvente(Node no) {
        Node pai = no.getParentNode().orElse(null);
        while (pai != null) {
            if (pai instanceof TypeDeclaration<?> tipo) {
                return Optional.of(tipo);
            }
            pai = pai.getParentNode().orElse(null);
        }
        return Optional.empty();
    }
}
