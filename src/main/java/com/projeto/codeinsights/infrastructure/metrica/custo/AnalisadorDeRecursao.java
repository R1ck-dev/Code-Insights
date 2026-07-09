package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

/**
 * Extrai de um metodo recursivo a <b>relacao de recorrencia</b> que ele descreve.
 * <p>
 * Este e o coracao da correcao do motor. A versao anterior contava ocorrencias
 * sintaticas do nome do metodo e concluia "2 ou mais auto-chamadas = O(2^n)". Isso
 * confunde duas coisas muito diferentes:
 * <ul>
 *   <li>quantas auto-chamadas <b>executam num mesmo caminho</b> ({@code a}) - a busca
 *       binaria recursiva tem duas ocorrencias, mas apenas uma executa, porque estao em
 *       ramos exclusivos de um {@code if};</li>
 *   <li><b>quanto o argumento encolhe</b> a cada nivel - sem isso, {@code 2T(n/2)+O(n)}
 *       (merge sort, {@code n log n}) e indistinguivel de {@code 2T(n-1)+O(1)}
 *       (Fibonacci ingenuo, {@code 2^n}).</li>
 * </ul>
 * Aqui medimos as duas coisas; {@link SolucionadorDeRecorrencia} resolve a recorrencia.
 */
public final class AnalisadorDeRecursao {

    /** Como o argumento da auto-chamada encolhe a cada nivel da recursao. */
    public enum Reducao {
        /** {@code f(n/b)} ou intervalo dividido ao meio: divisao e conquista. */
        DIVISIVA,
        /** {@code f(n-c)}: a recursao desce um degrau por vez. */
        SUBTRATIVA,
        /** {@code f(no.esq)}: percorre uma estrutura, visitando cada elemento uma vez. */
        ESTRUTURAL,
        /** Auto-chamada dentro de um laco sobre um intervalo: busca exaustiva. */
        BACKTRACKING,
        /** O argumento nao encolhe de forma reconhecivel. */
        INDETERMINADA
    }

    public record Recursao(Reducao reducao, int chamadasPorCaminho, int fator,
            boolean memoizada, int dimensoesDoCache, String suposicao) {

        public boolean temSuposicao() {
            return suposicao != null;
        }
    }

    private static final Set<String> TIPOS_NUMERICOS = Set.of("int", "long", "Integer", "Long", "short", "byte");
    private static final Set<String> CACHES_ASSOCIATIVOS = Set.of(
            "Map", "HashMap", "TreeMap", "LinkedHashMap", "Set", "HashSet", "TreeSet", "LinkedHashSet");
    private static final Set<String> CONSULTAS_DE_CACHE = Set.of("get", "containsKey", "contains", "getOrDefault");

    private AnalisadorDeRecursao() {
    }

    public static boolean ehRecursivo(MethodDeclaration metodo) {
        return !autoChamadas(metodo).isEmpty();
    }

    public static Recursao analisar(MethodDeclaration metodo) {
        List<MethodCallExpr> chamadas = autoChamadas(metodo);
        int chamadasPorCaminho = Math.max(1, contarPorCaminho(metodo.getBody().orElse(null), metodo));
        Recursao recursao = classificarReducao(metodo, chamadas, chamadasPorCaminho);

        return dimensoesDoCache(metodo)
                .map(dimensoes -> new Recursao(recursao.reducao(), chamadasPorCaminho, recursao.fator(), true, dimensoes,
                        "cache/marcacao de visitados detectado: o numero de estados distintos limita a recursao"))
                .orElse(recursao);
    }

    private static List<MethodCallExpr> autoChamadas(MethodDeclaration metodo) {
        return metodo.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> AstUtils.ehAutoChamada(chamada, metodo))
                .toList();
    }

    // ----- a: auto-chamadas no caminho de execucao mais custoso -----

    /**
     * Numero maximo de auto-chamadas que executam num unico caminho. Ramos de {@code if}
     * sao exclusivos (usa {@code max}); comandos em sequencia se somam - mas so ate o
     * primeiro que sempre retorna, porque o resto e inalcancavel naquele caminho.
     */
    private static int contarPorCaminho(Node no, MethodDeclaration metodo) {
        if (no == null) {
            return 0;
        }
        if (no instanceof MethodCallExpr chamada && AstUtils.ehAutoChamada(chamada, metodo)) {
            return 1 + somaDosFilhos(chamada.getArguments(), metodo);
        }
        if (no instanceof BlockStmt bloco) {
            return contarBloco(bloco.getStatements(), metodo);
        }
        if (no instanceof IfStmt condicional) {
            int entao = contarPorCaminho(condicional.getThenStmt(), metodo);
            int senao = condicional.getElseStmt().map(ramo -> contarPorCaminho(ramo, metodo)).orElse(0);
            return contarPorCaminho(condicional.getCondition(), metodo) + Math.max(entao, senao);
        }
        if (no instanceof ConditionalExpr ternario) {
            return contarPorCaminho(ternario.getCondition(), metodo)
                    + Math.max(contarPorCaminho(ternario.getThenExpr(), metodo),
                            contarPorCaminho(ternario.getElseExpr(), metodo));
        }
        return somaDosFilhos(no.getChildNodes(), metodo);
    }

    private static int contarBloco(List<Statement> comandos, MethodDeclaration metodo) {
        int acumulado = 0;
        int melhorCaminhoQueSai = 0;
        for (Statement comando : comandos) {
            if (ehGuardaQueRetorna(comando)) {
                IfStmt guarda = comando.asIfStmt();
                int custoDaGuarda = contarPorCaminho(guarda.getCondition(), metodo);
                melhorCaminhoQueSai = Math.max(melhorCaminhoQueSai,
                        acumulado + custoDaGuarda + contarPorCaminho(guarda.getThenStmt(), metodo));
                acumulado += custoDaGuarda;
                continue;
            }
            if (AstUtils.sempreRetorna(comando)) {
                return Math.max(melhorCaminhoQueSai, acumulado + contarPorCaminho(comando, metodo));
            }
            acumulado += contarPorCaminho(comando, metodo);
        }
        return Math.max(melhorCaminhoQueSai, acumulado);
    }

    private static boolean ehGuardaQueRetorna(Statement comando) {
        return comando instanceof IfStmt condicional
                && condicional.getElseStmt().isEmpty()
                && AstUtils.sempreRetorna(condicional.getThenStmt());
    }

    private static int somaDosFilhos(List<? extends Node> filhos, MethodDeclaration metodo) {
        return filhos.stream().mapToInt(filho -> contarPorCaminho(filho, metodo)).sum();
    }

    // ----- b: como o argumento encolhe -----

    private static Recursao classificarReducao(MethodDeclaration metodo, List<MethodCallExpr> chamadas, int a) {
        Optional<Recursao> emLaco = reducaoDeChamadaEmLaco(metodo, chamadas, a);
        if (emLaco.isPresent()) {
            return emLaco.get();
        }

        Set<String> parametros = nomesDeParametros(metodo);
        Set<String> pontosMedios = pontosMedios(metodo, parametros);
        Set<String> locais = nomesDeLocais(metodo, parametros);

        boolean divisiva = false;
        boolean subtrativa = false;
        boolean estrutural = false;
        boolean derivadoDeLocal = false;
        int fatorDivisao = Integer.MAX_VALUE;

        for (MethodCallExpr chamada : chamadas) {
            for (Expression argumento : chamada.getArguments()) {
                OptionalInt divisor = divisaoDeParametro(argumento, parametros);
                if (AstUtils.referenciaAlguma(argumento, pontosMedios)) {
                    divisiva = true;
                    fatorDivisao = Math.min(fatorDivisao, 2);
                } else if (divisor.isPresent()) {
                    divisiva = true;
                    fatorDivisao = Math.min(fatorDivisao, divisor.getAsInt());
                } else if (subtracaoDeParametro(argumento, parametros)) {
                    subtrativa = true;
                } else if (ehAcessoEstrutural(argumento, parametros)) {
                    estrutural = true;
                } else if (AstUtils.referenciaAlguma(argumento, locais)) {
                    derivadoDeLocal = true;
                }
            }
        }

        if (divisiva) {
            String suposicao = subtrativa
                    ? "reducao mista (n/b e n-c) nas auto-chamadas; assumida a divisiva, que domina"
                    : null;
            return new Recursao(Reducao.DIVISIVA, a, fatorDivisao, false, 0, suposicao);
        }
        if (subtrativa) {
            return new Recursao(Reducao.SUBTRATIVA, a, 1, false, 0, null);
        }
        if (estrutural) {
            return new Recursao(Reducao.ESTRUTURAL, a, 2, false, 0,
                    "recursao sobre uma estrutura de dados; assumido um passo por elemento");
        }
        if (derivadoDeLocal && a >= 2 && contarParametrosNumericos(metodo) >= 2) {
            return new Recursao(Reducao.DIVISIVA, a, 2, false, 0,
                    "recursao sobre subintervalos delimitados por um pivo; assumida divisao balanceada");
        }
        return new Recursao(Reducao.INDETERMINADA, a, 2, false, 0, null);
    }

    /** Auto-chamada dentro de laco: percorre vizinhos (estrutural) ou explora combinacoes (backtracking). */
    private static Optional<Recursao> reducaoDeChamadaEmLaco(MethodDeclaration metodo,
            List<MethodCallExpr> chamadas, int a) {
        Optional<Node> laco = chamadas.stream()
                .map(AstUtils::lacoMaisProximo)
                .filter(Objects::nonNull)
                .findFirst();
        if (laco.isEmpty()) {
            return Optional.empty();
        }
        if (laco.get() instanceof ForEachStmt paraCada && iteraSobreOsArgumentos(paraCada, chamadas)) {
            return Optional.of(new Recursao(Reducao.ESTRUTURAL, a, 2, false, 0,
                    "recursao sobre os elementos iterados; assumido um passo por elemento"));
        }
        return Optional.of(new Recursao(Reducao.BACKTRACKING, a, 2, false, 0,
                "auto-chamada dentro de laco: assumida busca exaustiva"));
    }

    private static boolean iteraSobreOsArgumentos(ForEachStmt paraCada, List<MethodCallExpr> chamadas) {
        String variavel = paraCada.getVariable().getVariable(0).getNameAsString();
        return chamadas.stream()
                .flatMap(chamada -> chamada.getArguments().stream())
                .anyMatch(argumento -> AstUtils.referenciaAlguma(argumento, Set.of(variavel)));
    }

    /** {@code f(n / 2)}, {@code f(n >> 1)}: devolve o divisor. */
    private static OptionalInt divisaoDeParametro(Expression argumento, Set<String> parametros) {
        for (BinaryExpr binaria : argumento.findAll(BinaryExpr.class)) {
            if (!binaria.getRight().isIntegerLiteralExpr() || !AstUtils.referenciaAlguma(binaria.getLeft(), parametros)) {
                continue;
            }
            int literal = binaria.getRight().asIntegerLiteralExpr().asNumber().intValue();
            if (binaria.getOperator() == BinaryExpr.Operator.DIVIDE && literal >= 2) {
                return OptionalInt.of(literal);
            }
            if (ehDeslocamentoADireita(binaria.getOperator()) && literal >= 1) {
                return OptionalInt.of(1 << literal);
            }
        }
        return OptionalInt.empty();
    }

    private static boolean ehDeslocamentoADireita(BinaryExpr.Operator operador) {
        return operador == BinaryExpr.Operator.SIGNED_RIGHT_SHIFT
                || operador == BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT;
    }

    /** {@code f(n - 1)} ou {@code f(--n)}. */
    private static boolean subtracaoDeParametro(Expression argumento, Set<String> parametros) {
        boolean porBinaria = argumento.findAll(BinaryExpr.class).stream()
                .anyMatch(binaria -> binaria.getOperator() == BinaryExpr.Operator.MINUS
                        && binaria.getRight().isIntegerLiteralExpr()
                        && AstUtils.referenciaAlguma(binaria.getLeft(), parametros));
        boolean porUnaria = argumento.findAll(UnaryExpr.class).stream()
                .filter(unaria -> unaria.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT
                        || unaria.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT)
                .anyMatch(unaria -> AstUtils.referenciaAlguma(unaria.getExpression(), parametros));
        return porBinaria || porUnaria;
    }

    /** {@code f(no.esq)}, {@code f(no.getEsq())}: desce numa estrutura em vez de num numero. */
    private static boolean ehAcessoEstrutural(Expression argumento, Set<String> parametros) {
        boolean acesso = argumento instanceof FieldAccessExpr || argumento instanceof MethodCallExpr;
        return acesso && AstUtils.referenciaAlguma(argumento, parametros);
    }

    /** Variaveis do metodo que nascem de uma divisao por constante sobre os parametros. */
    private static Set<String> pontosMedios(MethodDeclaration metodo, Set<String> parametros) {
        Set<String> medios = new HashSet<>();
        metodo.findAll(VariableDeclarator.class).forEach(declaracao -> declaracao.getInitializer()
                .filter(valor -> AstUtils.contemDivisaoPorConstante(valor)
                        && AstUtils.referenciaAlguma(valor, parametros))
                .ifPresent(valor -> medios.add(declaracao.getNameAsString())));
        return medios;
    }

    private static Set<String> nomesDeParametros(MethodDeclaration metodo) {
        return metodo.getParameters().stream().map(Parameter::getNameAsString).collect(Collectors.toSet());
    }

    private static Set<String> nomesDeLocais(MethodDeclaration metodo, Set<String> parametros) {
        return metodo.findAll(VariableDeclarator.class).stream()
                .map(VariableDeclarator::getNameAsString)
                .filter(nome -> !parametros.contains(nome))
                .collect(Collectors.toSet());
    }

    private static long contarParametrosNumericos(MethodDeclaration metodo) {
        return metodo.getParameters().stream()
                .filter(parametro -> TIPOS_NUMERICOS.contains(parametro.getType().asString()))
                .count();
    }

    // ----- memoizacao / marcacao de visitados -----

    /**
     * Detecta o idioma {@code if (memo[n] != VAZIO) return memo[n]; ... memo[n] = ...}
     * (e o equivalente com {@code visitado[u]}): a arvore de recursao e podada, e o custo
     * passa a ser o numero de estados distintos - a dimensao do cache - vezes o trabalho
     * por estado. E o que separa Fibonacci ingenuo, {@code O(2^n)}, do memoizado, {@code O(n)}.
     */
    private static Optional<Integer> dimensoesDoCache(MethodDeclaration metodo) {
        return candidatosACache(metodo).entrySet().stream()
                .filter(candidato -> temGuardaDeLeitura(metodo, candidato.getKey()))
                .filter(candidato -> temEscrita(metodo, candidato.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /** Arrays e mapas visiveis ao metodo e que sobrevivem entre chamadas: parametros e campos. */
    private static Map<String, Integer> candidatosACache(MethodDeclaration metodo) {
        Map<String, Integer> candidatos = new HashMap<>();
        metodo.getParameters().forEach(parametro -> dimensoesDe(parametro.getType())
                .ifPresent(dimensoes -> candidatos.put(parametro.getNameAsString(), dimensoes)));
        metodo.findAncestor(ClassOrInterfaceDeclaration.class).ifPresent(classe -> classe.getFields().stream()
                .flatMap(campo -> campo.getVariables().stream())
                .forEach(variavel -> dimensoesDe(variavel.getType())
                        .ifPresent(dimensoes -> candidatos.put(variavel.getNameAsString(), dimensoes))));
        return candidatos;
    }

    private static Optional<Integer> dimensoesDe(Type tipo) {
        if (tipo instanceof ArrayType arranjo) {
            return Optional.of(1 + dimensoesDe(arranjo.getComponentType()).orElse(0));
        }
        if (tipo instanceof ClassOrInterfaceType classe && CACHES_ASSOCIATIVOS.contains(classe.getNameAsString())) {
            return Optional.of(1);
        }
        return Optional.empty();
    }

    /**
     * A guarda precisa <b>consultar o cache pela chave</b> ({@code memo[n]}, {@code memo.get(n)}),
     * nao apenas mencionar a estrutura. Sem essa exigencia, {@code if (k == v.length) return;} num
     * backtracking que escreve {@code v[k]} seria lido como memoizacao, e uma busca exponencial
     * viraria O(n).
     */
    private static boolean temGuardaDeLeitura(MethodDeclaration metodo, String cache) {
        return metodo.findAll(IfStmt.class).stream()
                .filter(condicional -> AstUtils.sempreRetorna(condicional.getThenStmt()))
                .anyMatch(condicional -> consultaOCache(condicional.getCondition(), cache));
    }

    private static boolean consultaOCache(Expression condicao, String cache) {
        boolean porIndice = condicao.findAll(ArrayAccessExpr.class).stream()
                .anyMatch(acesso -> AstUtils.referenciaAlguma(acesso.getName(), Set.of(cache)));
        boolean porConsulta = condicao.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> CONSULTAS_DE_CACHE.contains(chamada.getNameAsString()))
                .anyMatch(chamada -> chamada.getScope()
                        .map(escopo -> escopo instanceof NameExpr nome && nome.getNameAsString().equals(cache))
                        .orElse(false));
        return porIndice || porConsulta;
    }

    private static boolean temEscrita(MethodDeclaration metodo, String cache) {
        boolean porAtribuicao = metodo.findAll(AssignExpr.class).stream()
                .anyMatch(atribuicao -> escreveEm(atribuicao.getTarget(), cache));
        boolean porChamada = metodo.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> chamada.getNameAsString().equals("put") || chamada.getNameAsString().equals("add"))
                .anyMatch(chamada -> chamada.getScope()
                        .map(escopo -> escopo instanceof NameExpr nome && nome.getNameAsString().equals(cache))
                        .orElse(false));
        return porAtribuicao || porChamada;
    }

    private static boolean escreveEm(Expression alvo, String cache) {
        if (alvo instanceof ArrayAccessExpr acesso) {
            return AstUtils.referenciaAlguma(acesso.getName(), Set.of(cache));
        }
        return alvo instanceof NameExpr nome && nome.getNameAsString().equals(cache);
    }
}
