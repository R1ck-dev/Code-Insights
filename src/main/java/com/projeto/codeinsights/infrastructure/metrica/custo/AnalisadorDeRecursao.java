package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

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

    public record Recursao(Reducao reducao, int chamadasPorCaminho, int fator, UsoDeCache cache, String suposicao) {

        public Recursao {
            Objects.requireNonNull(cache);
        }

        /** Assinatura anterior a {@link UsoDeCache}, mantida para o motor de C, que nao detecta cache. */
        public Recursao(Reducao reducao, int chamadasPorCaminho, int fator,
                boolean memoizada, int dimensoesDoCache, String suposicao) {
            this(reducao, chamadasPorCaminho, fator, memoizada
                    ? new UsoDeCache(UsoDeCache.Papel.MEMOIZACAO, dimensoesDoCache, UsoDeCache.Forma.GUARDA_QUE_RETORNA)
                    : UsoDeCache.NENHUM, suposicao);
        }

        /** O cache poda a arvore: o tempo e estados distintos x trabalho por estado. */
        public boolean memoizada() {
            return cache.podaOTempo();
        }

        public int dimensoesDoCache() {
            return cache.dimensoes();
        }

        public boolean temSuposicao() {
            return suposicao != null;
        }
    }

    private static final Set<String> TIPOS_NUMERICOS = Set.of("int", "long", "Integer", "Long", "short", "byte");
    private static final String SUPOSICAO_DE_CACHE =
            "cache/marcacao de visitados detectado: o numero de estados distintos limita a recursao";

    private AnalisadorDeRecursao() {
    }

    public static boolean ehRecursivo(MethodDeclaration metodo, ResolvedorDeChamadas resolvedor) {
        return !autoChamadas(metodo, resolvedor).isEmpty();
    }

    public static Recursao analisar(MethodDeclaration metodo, ResolvedorDeChamadas resolvedor) {
        List<MethodCallExpr> chamadas = autoChamadas(metodo, resolvedor);
        Set<MethodCallExpr> proprias = ResolvedorDeChamadas.conjuntoPorIdentidade();
        proprias.addAll(chamadas);
        int chamadasPorCaminho = Math.max(1, contarPorCaminho(metodo.getBody().orElse(null), proprias));
        Recursao recursao = classificarReducao(metodo, chamadas, chamadasPorCaminho, resolvedor);
        return comCache(recursao, ClassificadorDeCache.classificar(metodo, chamadas));
    }

    /**
     * Anexa o papel do cache a recorrencia. A reducao continua a mesma; quem decide se ela ainda
     * vale e o consumidor ({@link SolucionadorDeRecorrencia} para o tempo, {@link AvaliadorDeEspaco}
     * para a pilha). A suposicao sobre estados distintos so entra quando o cache poda o tempo.
     */
    private static Recursao comCache(Recursao recursao, UsoDeCache cache) {
        if (cache.papel() == UsoDeCache.Papel.NENHUM) {
            return recursao;
        }
        String suposicao = cache.podaOTempo() ? SUPOSICAO_DE_CACHE : recursao.suposicao();
        return new Recursao(recursao.reducao(), recursao.chamadasPorCaminho(), recursao.fator(), cache, suposicao);
    }

    /** As chamadas do metodo a si mesmo — pelo nome, por {@code this}, pela classe ou por outro objeto da mesma classe. */
    private static List<MethodCallExpr> autoChamadas(MethodDeclaration metodo, ResolvedorDeChamadas resolvedor) {
        return metodo.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> resolvedor.ehAutoChamada(chamada, metodo))
                .toList();
    }

    // ----- a: auto-chamadas no caminho de execucao mais custoso -----

    /**
     * Numero maximo de auto-chamadas que executam num unico caminho. Ramos de {@code if}
     * sao exclusivos (usa {@code max}); comandos em sequencia se somam - mas so ate o
     * primeiro que sempre retorna, porque o resto e inalcancavel naquele caminho.
     */
    private static int contarPorCaminho(Node no, Set<MethodCallExpr> proprias) {
        if (no == null) {
            return 0;
        }
        if (no instanceof MethodCallExpr chamada && proprias.contains(chamada)) {
            return 1 + somaDosFilhos(chamada.getArguments(), proprias);
        }
        if (no instanceof BlockStmt bloco) {
            return contarBloco(bloco.getStatements(), proprias);
        }
        if (no instanceof IfStmt condicional) {
            int entao = contarPorCaminho(condicional.getThenStmt(), proprias);
            int senao = condicional.getElseStmt().map(ramo -> contarPorCaminho(ramo, proprias)).orElse(0);
            return contarPorCaminho(condicional.getCondition(), proprias) + Math.max(entao, senao);
        }
        if (no instanceof ConditionalExpr ternario) {
            return contarPorCaminho(ternario.getCondition(), proprias)
                    + Math.max(contarPorCaminho(ternario.getThenExpr(), proprias),
                            contarPorCaminho(ternario.getElseExpr(), proprias));
        }
        return somaDosFilhos(no.getChildNodes(), proprias);
    }

    private static int contarBloco(List<Statement> comandos, Set<MethodCallExpr> proprias) {
        int acumulado = 0;
        int melhorCaminhoQueSai = 0;
        for (Statement comando : comandos) {
            if (ehGuardaQueRetorna(comando)) {
                IfStmt guarda = comando.asIfStmt();
                int custoDaGuarda = contarPorCaminho(guarda.getCondition(), proprias);
                melhorCaminhoQueSai = Math.max(melhorCaminhoQueSai,
                        acumulado + custoDaGuarda + contarPorCaminho(guarda.getThenStmt(), proprias));
                acumulado += custoDaGuarda;
                continue;
            }
            if (AstUtils.sempreRetorna(comando)) {
                return Math.max(melhorCaminhoQueSai, acumulado + contarPorCaminho(comando, proprias));
            }
            acumulado += contarPorCaminho(comando, proprias);
        }
        return Math.max(melhorCaminhoQueSai, acumulado);
    }

    private static boolean ehGuardaQueRetorna(Statement comando) {
        return comando instanceof IfStmt condicional
                && condicional.getElseStmt().isEmpty()
                && AstUtils.sempreRetorna(condicional.getThenStmt());
    }

    private static int somaDosFilhos(List<? extends Node> filhos, Set<MethodCallExpr> proprias) {
        return filhos.stream().mapToInt(filho -> contarPorCaminho(filho, proprias)).sum();
    }

    // ----- b: como o argumento encolhe -----

    private static Recursao classificarReducao(MethodDeclaration metodo, List<MethodCallExpr> chamadas, int a,
            ResolvedorDeChamadas resolvedor) {
        Optional<Recursao> emLaco = reducaoDeChamadaEmLaco(metodo, chamadas, a);
        if (emLaco.isPresent()) {
            return emLaco.get();
        }

        Set<String> parametros = nomesDeParametros(metodo);
        Set<String> pontosMedios = pontosMedios(metodo, parametros);
        Set<String> locais = nomesDeLocais(metodo, parametros);

        boolean divisiva = false;
        boolean porResto = false;
        boolean subtrativa = false;
        boolean estrutural = false;
        boolean derivadoDeLocal = false;
        int fatorDivisao = Integer.MAX_VALUE;

        for (MethodCallExpr chamada : chamadas) {
            // esq.altura(): o argumento nao encolhe, o receptor desce na estrutura
            estrutural |= resolvedor.chamaOutraInstancia(chamada);
            for (Expression argumento : chamada.getArguments()) {
                OptionalInt divisor = divisaoDeParametro(argumento, parametros);
                if (AstUtils.referenciaAlguma(argumento, pontosMedios)) {
                    divisiva = true;
                    fatorDivisao = Math.min(fatorDivisao, 2);
                } else if (divisor.isPresent()) {
                    divisiva = true;
                    fatorDivisao = Math.min(fatorDivisao, divisor.getAsInt());
                } else if (restoEntreParametros(argumento, parametros)) {
                    divisiva = true;
                    porResto = true;
                    fatorDivisao = Math.min(fatorDivisao, 2);
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
            return new Recursao(Reducao.DIVISIVA, a, fatorDivisao, UsoDeCache.NENHUM,
                    suposicaoDaDivisao(subtrativa, porResto));
        }
        if (subtrativa) {
            return new Recursao(Reducao.SUBTRATIVA, a, 1, UsoDeCache.NENHUM, null);
        }
        if (estrutural) {
            return new Recursao(Reducao.ESTRUTURAL, a, 2, UsoDeCache.NENHUM,
                    "recursao sobre uma estrutura de dados; assumido um passo por elemento");
        }
        if (derivadoDeLocal && a >= 2 && contarParametrosNumericos(metodo) >= 2) {
            return new Recursao(Reducao.DIVISIVA, a, 2, UsoDeCache.NENHUM,
                    "recursao sobre subintervalos delimitados por um pivo; assumida divisao balanceada");
        }
        return new Recursao(Reducao.INDETERMINADA, a, 2, UsoDeCache.NENHUM, null);
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
        if (laco.get() instanceof ForEachStmt paraCada && iteraSobreOsOperandos(paraCada, chamadas)) {
            return Optional.of(new Recursao(Reducao.ESTRUTURAL, a, 2, UsoDeCache.NENHUM,
                    "recursao sobre os elementos iterados; assumido um passo por elemento"));
        }
        return Optional.of(new Recursao(Reducao.BACKTRACKING, a, 2, UsoDeCache.NENHUM,
                "auto-chamada dentro de laco: assumida busca exaustiva"));
    }

    /** A variavel do for-each e passada como argumento ({@code f(viz)}) ou e o receptor ({@code filho.f()}). */
    private static boolean iteraSobreOsOperandos(ForEachStmt paraCada, List<MethodCallExpr> chamadas) {
        String variavel = paraCada.getVariable().getVariable(0).getNameAsString();
        return chamadas.stream()
                .flatMap(chamada -> Stream.concat(chamada.getArguments().stream(), chamada.getScope().stream()))
                .anyMatch(operando -> AstUtils.referenciaAlguma(operando, Set.of(variavel)));
    }

    private static String suposicaoDaDivisao(boolean subtrativa, boolean porResto) {
        if (subtrativa) {
            return "reducao mista (n/b e n-c) nas auto-chamadas; assumida a divisiva, que domina";
        }
        if (porResto) {
            return "reducao por resto entre parametros, como no algoritmo de Euclides; "
                    + "assumida queda geometrica do argumento";
        }
        return null;
    }

    /**
     * {@code f(b, a % b)}: o argumento cai por resto, e nao por divisao explicita. E a forma do
     * algoritmo de Euclides, cujo numero de passos e logaritmico (Lame). Sem esta regra o motor
     * classificava a reducao como INDETERMINADA e <b>recusava responder</b> um dos algoritmos mais
     * comuns em programacao competitiva.
     * <p>
     * A regra e estreita de proposito — exige que <b>os dois</b> lados do {@code %} sejam
     * parametros. {@code f(n % 2)}, com literal a direita, nao e reducao geometrica: cai para uma
     * constante num passo so.
     */
    private static boolean restoEntreParametros(Expression argumento, Set<String> parametros) {
        return argumento.findAll(BinaryExpr.class).stream()
                .anyMatch(binaria -> binaria.getOperator() == BinaryExpr.Operator.REMAINDER
                        && AstUtils.referenciaAlguma(binaria.getLeft(), parametros)
                        && AstUtils.referenciaAlguma(binaria.getRight(), parametros));
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
}
