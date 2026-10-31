package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
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
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
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

    /** A referencia a metodo ({@code Main::f}) nao traz argumentos: a aridade nao serve para escolher o metodo. */
    private static final int QUALQUER_ARIDADE = -1;

    private final TiposDeVariavel tipos;
    private final NomesAmbiguos ambiguos;
    private final List<MethodDeclaration> metodos;
    private final Map<String, TypeDeclaration<?>> classes = new HashMap<>();

    private ResolvedorDeChamadas(CompilationUnit unidade) {
        this.tipos = TiposDeVariavel.de(unidade);
        this.ambiguos = NomesAmbiguos.de(unidade);
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
        return chamadosPor(metodo, chamada -> true);
    }

    /**
     * Como {@link #chamadosPor}, mas sem as chamadas cujo receptor e um nome declarado com mais de um
     * tipo na unidade: a tabela plana resolve esse receptor para um dos tipos, sem saber qual, e a
     * aresta pode ser falsa. Alimenta so o SCC do tempo, onde uma aresta falsa vira um numero errado;
     * o espaco continua com {@link #chamadosPor}.
     */
    public Set<MethodDeclaration> chamadosPorSemReceptorAmbiguo(MethodDeclaration metodo) {
        return chamadosPor(metodo, chamada -> chamada.getScope().filter(ambiguos::receptorAmbiguo).isEmpty());
    }

    private Set<MethodDeclaration> chamadosPor(MethodDeclaration metodo, Predicate<MethodCallExpr> considerar) {
        Set<MethodDeclaration> chamados = conjuntoPorIdentidade();
        metodo.findAll(MethodCallExpr.class).stream()
                .filter(considerar)
                .forEach(chamada -> alvo(chamada).filter(alvo -> alvo != metodo).ifPresent(chamados::add));
        return chamados;
    }

    /**
     * Metodos da unidade que a <b>referencia</b> {@code X::m} / {@code this::m} / {@code receptor::m} pode
     * apontar. Como ela nao traz argumentos, todas as sobrecargas de nome {@code m} na classe alvo sao
     * candidatas; quem custeia escolhe entre elas. Vazio para biblioteca ({@code Integer::sum}), para
     * construtor ({@code Tipo::new}) e para receptor de tipo que o motor nao sabe dizer.
     */
    public List<MethodDeclaration> alvosDaReferencia(MethodReferenceExpr referencia) {
        String nome = referencia.getIdentifier();
        Expression escopo = referencia.getScope();
        if (escopo instanceof ThisExpr) {
            return candidatosSemReceptor(referencia, nome, QUALQUER_ARIDADE);
        }
        return classeDaReferencia(escopo)
                .map(classe -> candidatosDe(classe, nome, QUALQUER_ARIDADE, conjuntoPorIdentidade()))
                .orElse(List.of());
    }

    /**
     * Metodos da unidade que {@code metodo} aponta por referencia; a auto-referencia nao conta. Fica
     * fora de {@link #chamadosPor}: o grafo de ciclos do espaco nao enxerga referencias, e so a escolha
     * do ponto de entrada precisa saber que um metodo so referenciado nao e raiz.
     */
    public Set<MethodDeclaration> referenciadosPor(MethodDeclaration metodo) {
        Set<MethodDeclaration> referenciados = conjuntoPorIdentidade();
        metodo.findAll(MethodReferenceExpr.class)
                .forEach(referencia -> alvosDaReferencia(referencia).stream()
                        .filter(alvo -> alvo != metodo)
                        .forEach(referenciados::add));
        return referenciados;
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
        return ultimo(candidatosSemReceptor(chamada, nome, aridade));
    }

    private List<MethodDeclaration> candidatosSemReceptor(Node ponto, String nome, int aridade) {
        Optional<TypeDeclaration<?>> classe = tipoEnvolvente(ponto);
        while (classe.isPresent()) {
            List<MethodDeclaration> achados = candidatosDe(classe.get(), nome, aridade, conjuntoPorIdentidade());
            if (!achados.isEmpty()) {
                return achados;
            }
            classe = tipoEnvolvente(classe.get());
        }
        // Corpo de classe anonima nao e um TypeDeclaration: ali o metodo so se acha pelo nome.
        return metodos.stream().filter(metodo -> ehMetodo(metodo, nome, aridade)).toList();
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
        return ultimo(candidatosDe(tipo, nome, aridade, conjuntoPorIdentidade()));
    }

    /**
     * {@code X} em {@code X::m}. O parser entrega tudo como {@code TypeExpr} — {@code Main::f} e
     * {@code filtro::passa} —, entao um nome que e variavel declarada resolve pelo tipo dela, e o resto
     * pelo nome da classe da unidade. Receptor mais complexo ({@code obj.campo::m}) segue a regra da chamada.
     */
    private Optional<TypeDeclaration<?>> classeDaReferencia(Expression escopo) {
        if (!(escopo instanceof TypeExpr tipo)) {
            return classeDoReceptor(escopo);
        }
        Optional<String> nome = TiposDeVariavel.nomeDe(tipo.getType());
        Optional<Type> daVariavel = nome.flatMap(tipos::tipoDeclarado);
        return (daVariavel.isPresent() ? daVariavel.flatMap(TiposDeVariavel::nomeDe) : nome).map(classes::get);
    }

    /**
     * Os metodos de nome {@code nome} que o tipo declara; se nao declara nenhum, os do primeiro
     * supertipo da unidade que declare. Sobrecargas de mesma aridade ficam todas na lista.
     */
    private List<MethodDeclaration> candidatosDe(TypeDeclaration<?> tipo, String nome, int aridade,
            Set<TypeDeclaration<?>> vistos) {
        if (!vistos.add(tipo)) {
            return List.of();
        }
        List<MethodDeclaration> proprios = tipo.getMethods().stream()
                .filter(metodo -> ehMetodo(metodo, nome, aridade))
                .toList();
        if (!proprios.isEmpty() || !(tipo instanceof ClassOrInterfaceDeclaration declaracao)) {
            return proprios;
        }
        return Stream.concat(declaracao.getExtendedTypes().stream(), declaracao.getImplementedTypes().stream())
                .map(supertipo -> classes.get(supertipo.getNameAsString()))
                .filter(Objects::nonNull)
                .map(supertipo -> candidatosDe(supertipo, nome, aridade, vistos))
                .filter(achados -> !achados.isEmpty())
                .findFirst()
                .orElse(List.of());
    }

    /** Entre sobrecargas de mesma aridade, vence a ultima declarada. */
    private static Optional<MethodDeclaration> ultimo(List<MethodDeclaration> candidatos) {
        return candidatos.isEmpty() ? Optional.empty() : Optional.of(candidatos.get(candidatos.size() - 1));
    }

    private static boolean ehMetodo(MethodDeclaration metodo, String nome, int aridade) {
        return metodo.getNameAsString().equals(nome)
                && (aridade == QUALQUER_ARIDADE || metodo.getParameters().size() == aridade);
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
