package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;

/**
 * Decide se um cache e <b>desfeito na volta</b>: o metodo marca o estado antes de chamar a si
 * mesmo e restaura o valor livre depois. Marcar e desmarcar e backtracking - o mesmo estado e
 * revisitado por caminhos diferentes - e nao memoizacao, mesmo quando a consulta ao cache poda
 * as auto-chamadas.
 * <p>
 * A regra e deliberadamente estreita (decisao do dono, 28/10: na duvida, nao declarar memo,
 * porque subestimar e o pior erro). Existe uma auto-chamada K, uma escrita no cache que comeca
 * antes de K e uma escrita que comeca depois do fim de K e que <b>restaura o valor livre</b>
 * sem <b>derivar da recursao</b>:
 * <ul>
 *   <li>{@code false}; o valor com que uma consulta ao cache e comparada ({@code != 0} libera 0,
 *       {@code != '#'} libera {@code '#'}); o valor salvo numa local ({@code tmp = b[i][j]} ...
 *       {@code b[i][j] = tmp}); {@code cache.remove(...)}; {@code cache.put(k, false)};</li>
 *   <li>fica de fora o que grava o resultado ({@code memo[n] = total}, {@code memo.put(k, total)})
 *       e a marca definitiva ({@code cor[u] = 2} na DFS de tres cores).</li>
 * </ul>
 * A ordem e a de fonte ({@code Node.getRange()}); num laco ela e uma aproximacao da ordem de execucao.
 * <p>
 * Nas <b>formas novas</b> de protecao (if envolvente, ternario, continue) a regra e mais larga, o modo
 * {@code conservador}: a consulta ali nao diz qual e o valor livre ({@code usado[j] == 1} libera 0, e a
 * guarda so compara com 1), entao desfaz tambem a escrita que nao deriva da recursao e grava uma
 * constante ({@code usado[j] = 0}, {@code put(k, 0)}), o toggle do proprio valor
 * ({@code usado[j] = !usado[j]}) e a chamada, depois da auto-chamada, a um metodo que recebe o cache
 * como argumento ({@code desmarca(usado, j)}). Na duvida, desfeita: subestimar e o pior erro. A guarda
 * que retorna segue na regra estreita.
 */
final class DesmarcacaoNaVolta {

    private static final Pattern CONSTANTE_NOMEADA = Pattern.compile("[A-Z][A-Z0-9_]*");
    private static final Set<String> ESCRITAS_POR_CHAMADA = Set.of("put", "add", "remove");

    private final String cache;
    private final boolean conservador;
    private final Set<MethodCallExpr> autoChamadas = ResolvedorDeChamadas.conjuntoPorIdentidade();
    private final List<Node> escritas;
    private final List<MethodCallExpr> auxiliaresQueRecebemOCache;
    private final List<Expression> valoresLivres;
    private final DerivacaoDaRecursao recursao;
    private final Set<String> locaisQueLeemOCache;

    private DesmarcacaoNaVolta(MethodDeclaration metodo, String cache, Collection<MethodCallExpr> autoChamadas,
            boolean conservador) {
        this.cache = cache;
        this.conservador = conservador;
        this.autoChamadas.addAll(autoChamadas);
        this.escritas = escritasEm(metodo);
        this.auxiliaresQueRecebemOCache = conservador ? auxiliaresQueRecebemOCache(metodo) : List.of();
        this.valoresLivres = valoresComparadosNaConsulta(metodo);
        this.recursao = new DerivacaoDaRecursao(metodo, autoChamadas);
        this.locaisQueLeemOCache = locaisQueLeemOCache(recursao.atribuicoes());
    }

    /**
     * Alguma auto-chamada tem uma escrita no cache antes e uma que restaura o valor livre depois?
     *
     * @param conservador a regra larga das formas novas de protecao; a guarda que retorna passa {@code false}
     */
    static boolean desfazNaVolta(MethodDeclaration metodo, String cache, Collection<MethodCallExpr> autoChamadas,
            boolean conservador) {
        return new DesmarcacaoNaVolta(metodo, cache, autoChamadas, conservador).desfaz();
    }

    private boolean desfaz() {
        return autoChamadas.stream().anyMatch(chamada ->
                escritas.stream().anyMatch(escrita -> comecaAntes(escrita, chamada)) && restauraDepoisDe(chamada));
    }

    private boolean restauraDepoisDe(MethodCallExpr chamada) {
        return escritas.stream().anyMatch(escrita -> comecaDepois(escrita, chamada) && restaura(escrita))
                || auxiliaresQueRecebemOCache.stream().anyMatch(auxiliar -> comecaDepois(auxiliar, chamada));
    }

    // ----- a ordem de fonte; sem posicao nao ha como provar a ordem, entao nao se desfaz -----

    private static boolean comecaAntes(Node escrita, Node chamada) {
        return escrita.getBegin().isPresent() && chamada.getBegin().isPresent()
                && escrita.getBegin().get().isBefore(chamada.getBegin().get());
    }

    private static boolean comecaDepois(Node escrita, Node chamada) {
        return escrita.getBegin().isPresent() && chamada.getEnd().isPresent()
                && escrita.getBegin().get().isAfter(chamada.getEnd().get());
    }

    // ----- as escritas e o que restaura -----

    /** Atribuicoes ao cache e {@code put}/{@code add}/{@code remove} sobre ele. */
    private List<Node> escritasEm(MethodDeclaration metodo) {
        List<Node> achadas = new ArrayList<>();
        metodo.findAll(AssignExpr.class).stream()
                .filter(atribuicao -> AcessosAoCache.escreveEm(atribuicao.getTarget(), cache))
                .forEach(achadas::add);
        metodo.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> ESCRITAS_POR_CHAMADA.contains(chamada.getNameAsString()))
                .filter(chamada -> AcessosAoCache.temEscopo(chamada, cache))
                .forEach(achadas::add);
        return achadas;
    }

    /** Chamadas que nao sao auto-chamadas e recebem o proprio cache como argumento: {@code desmarca(usado, j)}. */
    private List<MethodCallExpr> auxiliaresQueRecebemOCache(MethodDeclaration metodo) {
        return metodo.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> !autoChamadas.contains(chamada))
                .filter(chamada -> chamada.getArguments().stream().anyMatch(this::ehOCache))
                .toList();
    }

    private boolean ehOCache(Expression argumento) {
        return argumento instanceof NameExpr nome && nome.getNameAsString().equals(cache);
    }

    private boolean restaura(Node escrita) {
        if (escrita instanceof AssignExpr atribuicao) {
            return atribuicao.getOperator() == AssignExpr.Operator.ASSIGN && restauraValorLivre(atribuicao.getValue());
        }
        MethodCallExpr chamada = (MethodCallExpr) escrita;
        return switch (chamada.getNameAsString()) {
            case "remove" -> true;
            case "put" -> chamada.getArguments().size() == 2 && restauraNoPut(chamada.getArgument(1));
            default -> false;
        };
    }

    private boolean restauraValorLivre(Expression valor) {
        if (recursao.deriva(valor)) {
            return false;
        }
        return ehFalso(valor)
                || valoresLivres.contains(valor)
                || (valor instanceof NameExpr nome && locaisQueLeemOCache.contains(nome.getNameAsString()))
                || (conservador && ehConstanteOuInverso(valor));
    }

    private boolean restauraNoPut(Expression valor) {
        return ehFalso(valor) || (conservador && !recursao.deriva(valor) && ehConstanteOuInverso(valor));
    }

    /** Modo conservador: uma constante ou o inverso de uma celula do cache ({@code !usado[j]}, o toggle). */
    private boolean ehConstanteOuInverso(Expression valor) {
        boolean inverso = valor instanceof UnaryExpr negacao
                && negacao.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT
                && AcessosAoCache.consulta(negacao.getExpression(), cache);
        return ehConstante(valor) || inverso;
    }

    private static boolean ehFalso(Expression valor) {
        return valor.isBooleanLiteralExpr() && !valor.asBooleanLiteralExpr().getValue();
    }

    // ----- o valor livre: o literal ou a constante com que uma consulta ao cache e comparada -----

    private List<Expression> valoresComparadosNaConsulta(MethodDeclaration metodo) {
        List<Expression> valores = new ArrayList<>();
        for (BinaryExpr comparacao : metodo.findAll(BinaryExpr.class)) {
            boolean ehIgualdade = comparacao.getOperator() == BinaryExpr.Operator.EQUALS
                    || comparacao.getOperator() == BinaryExpr.Operator.NOT_EQUALS;
            if (ehIgualdade) {
                colherValorLivre(comparacao.getLeft(), comparacao.getRight(), valores);
                colherValorLivre(comparacao.getRight(), comparacao.getLeft(), valores);
            }
        }
        return valores;
    }

    private void colherValorLivre(Expression consulta, Expression valor, List<Expression> valores) {
        if (AcessosAoCache.consulta(consulta, cache) && ehConstante(valor)) {
            valores.add(valor);
        }
    }

    private static boolean ehConstante(Expression valor) {
        if (valor instanceof UnaryExpr negativo && negativo.getOperator() == UnaryExpr.Operator.MINUS) {
            return negativo.getExpression() instanceof LiteralExpr;
        }
        return valor instanceof LiteralExpr
                || (valor instanceof NameExpr nome && CONSTANTE_NOMEADA.matcher(nome.getNameAsString()).matches());
    }

    // ----- locais que guardam o valor do cache -----

    private Set<String> locaisQueLeemOCache(List<DerivacaoDaRecursao.Atribuicao> atribuicoes) {
        Set<String> locais = new HashSet<>();
        atribuicoes.stream()
                .filter(atribuicao -> AcessosAoCache.consulta(atribuicao.valor(), cache))
                .forEach(atribuicao -> locais.add(atribuicao.local()));
        return locais;
    }
}
