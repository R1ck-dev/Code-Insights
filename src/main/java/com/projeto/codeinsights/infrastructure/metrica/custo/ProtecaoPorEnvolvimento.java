package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.projeto.codeinsights.infrastructure.metrica.custo.UsoDeCache.Forma;

/**
 * A consulta ao cache protege as auto-chamadas por <b>envolve-las num {@code if}</b>, em vez de
 * retornar antes delas (esse e o caso de {@link ClassificadorDeCache}, a guarda que retorna).
 * <p>
 * Duas formas, as duas exigindo que <b>toda</b> auto-chamada esteja protegida:
 * <ul>
 *   <li>{@link Forma#CALCULA_SE_AUSENTE}: o ramo grava no cache um valor derivado da recursao,
 *       {@code if (memo[n] == 0) memo[n] = f(n - 1) + f(n - 2);}. A escrita do resultado e a prova de que
 *       o ramo e "calcular o que falta"; nao se exige nada da chave.</li>
 *   <li>{@link Forma#CHECA_ANTES_DE_CHAMAR}: o ramo so chama, {@code if (!vis[v]) visita(v);}. Sem
 *       escrita de resultado a prova e outra: a <b>regra do indice</b> - algum nome da chave consultada
 *       aparece nos argumentos da chamada, ou seja, o estado consultado e o estado visitado - e a
 *       consulta nao pode ser uma comparacao relacional ({@code <}, {@code >}), que compara dados em vez de
 *       testar uma marca. Barra {@code if (v[i] > v[i + 1]) { troca; ordena(v); }}.</li>
 * </ul>
 * A consulta tem de ser condicao <b>necessaria</b> do ramo: um conjunto {@code &&}, nunca um {@code ||}.
 * Ficam de fora, e superestimam: a chamada na propria condicao por curto-circuito
 * ({@code if (!vis[v] && dfs(v))}) e a consulta feita por um metodo auxiliar.
 */
final class ProtecaoPorEnvolvimento {

    private static final Set<BinaryExpr.Operator> RELACIONAIS = EnumSet.of(
            BinaryExpr.Operator.LESS, BinaryExpr.Operator.LESS_EQUALS,
            BinaryExpr.Operator.GREATER, BinaryExpr.Operator.GREATER_EQUALS);

    private final String cache;
    private final DerivacaoDaRecursao recursao;

    private ProtecaoPorEnvolvimento(MethodDeclaration metodo, String cache, Collection<MethodCallExpr> autoChamadas) {
        this.cache = cache;
        this.recursao = new DerivacaoDaRecursao(metodo, autoChamadas);
    }

    /** A forma que protege todas as auto-chamadas, ou {@link Forma#NENHUMA}. */
    static Forma forma(MethodDeclaration metodo, String cache, Collection<MethodCallExpr> autoChamadas) {
        return new ProtecaoPorEnvolvimento(metodo, cache, autoChamadas).formaQueProtegeTodas(autoChamadas);
    }

    private Forma formaQueProtegeTodas(Collection<MethodCallExpr> autoChamadas) {
        if (autoChamadas.isEmpty()) {
            return Forma.NENHUMA;
        }
        Forma geral = Forma.CALCULA_SE_AUSENTE;
        for (MethodCallExpr chamada : autoChamadas) {
            Optional<Forma> forma = formaQueProtege(chamada);
            if (forma.isEmpty()) {
                return Forma.NENHUMA;
            }
            if (forma.get() == Forma.CHECA_ANTES_DE_CHAMAR) {
                geral = Forma.CHECA_ANTES_DE_CHAMAR;
            }
        }
        return geral;
    }

    /** O {@code if} mais interno que protege a chamada; o que calcula-se-ausente vence o que so checa. */
    private Optional<Forma> formaQueProtege(MethodCallExpr chamada) {
        List<Forma> formas = ifsCujoRamoEntaoContem(chamada).stream()
                .map(condicional -> formaDo(condicional, chamada))
                .filter(forma -> forma != Forma.NENHUMA)
                .toList();
        if (formas.contains(Forma.CALCULA_SE_AUSENTE)) {
            return Optional.of(Forma.CALCULA_SE_AUSENTE);
        }
        return formas.stream().findFirst();
    }

    private static List<IfStmt> ifsCujoRamoEntaoContem(MethodCallExpr chamada) {
        List<IfStmt> envolventes = new ArrayList<>();
        Node filho = chamada;
        while (filho.getParentNode().isPresent() && !(filho instanceof MethodDeclaration)) {
            Node pai = filho.getParentNode().get();
            if (pai instanceof IfStmt condicional && condicional.getThenStmt() == filho) {
                envolventes.add(condicional);
            }
            filho = pai;
        }
        return envolventes;
    }

    private Forma formaDo(IfStmt condicional, MethodCallExpr chamada) {
        List<Expression> consultas = condicoesNecessarias(condicional.getCondition()).stream()
                .filter(condicao -> AcessosAoCache.consulta(condicao, cache))
                .toList();
        if (consultas.isEmpty()) {
            return Forma.NENHUMA;
        }
        if (gravaResultadoNoCache(condicional.getThenStmt())) {
            return Forma.CALCULA_SE_AUSENTE;
        }
        boolean testaMarcaDoEstadoChamado = consultas.stream()
                .anyMatch(consulta -> !ehRelacional(consulta) && chaveNosArgumentos(consulta, chamada));
        return testaMarcaDoEstadoChamado ? Forma.CHECA_ANTES_DE_CHAMAR : Forma.NENHUMA;
    }

    // ----- a condicao: o que e verdade sempre que o ramo executa -----

    /** Os conjuntos {@code &&} da condicao; um {@code ||} ou {@code !(a && b)} nao garante nenhum dos lados. */
    private static List<Expression> condicoesNecessarias(Expression condicao) {
        Expression nua = semParenteses(condicao);
        if (nua instanceof BinaryExpr binaria && binaria.getOperator() == BinaryExpr.Operator.AND) {
            List<Expression> conjuntos = new ArrayList<>(condicoesNecessarias(binaria.getLeft()));
            conjuntos.addAll(condicoesNecessarias(binaria.getRight()));
            return conjuntos;
        }
        return garanteAlgo(nua) ? List.of(nua) : List.of();
    }

    private static boolean garanteAlgo(Expression nua) {
        if (nua instanceof BinaryExpr binaria) {
            return binaria.getOperator() != BinaryExpr.Operator.OR;
        }
        if (nua instanceof UnaryExpr negacao && negacao.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
            Expression interna = semParenteses(negacao.getExpression());
            return !(interna instanceof BinaryExpr binaria && binaria.getOperator() == BinaryExpr.Operator.AND);
        }
        return true;
    }

    private static boolean ehRelacional(Expression condicao) {
        return semParenteses(condicao) instanceof BinaryExpr binaria && RELACIONAIS.contains(binaria.getOperator());
    }

    private static Expression semParenteses(Expression expressao) {
        Expression atual = expressao;
        while (atual instanceof EnclosedExpr entre) {
            atual = entre.getInner();
        }
        return atual;
    }

    // ----- a regra do indice e a escrita do resultado -----

    private boolean chaveNosArgumentos(Expression consulta, MethodCallExpr chamada) {
        Set<String> chave = AcessosAoCache.nomesDaChave(consulta, cache);
        return chamada.getArguments().stream()
                .flatMap(argumento -> AcessosAoCache.nomesEm(argumento).stream())
                .anyMatch(chave::contains);
    }

    /** O ramo atribui ao cache, ou faz {@code put}, um valor que vem da recursao. */
    private boolean gravaResultadoNoCache(Statement ramo) {
        boolean porAtribuicao = ramo.findAll(AssignExpr.class).stream()
                .filter(atribuicao -> atribuicao.getOperator() == AssignExpr.Operator.ASSIGN)
                .anyMatch(atribuicao -> AcessosAoCache.escreveEm(atribuicao.getTarget(), cache)
                        && recursao.deriva(atribuicao.getValue()));
        boolean porPut = ramo.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> chamada.getNameAsString().equals("put") && chamada.getArguments().size() == 2)
                .filter(chamada -> AcessosAoCache.temEscopo(chamada, cache))
                .anyMatch(chamada -> recursao.deriva(chamada.getArgument(1)));
        return porAtribuicao || porPut;
    }
}
