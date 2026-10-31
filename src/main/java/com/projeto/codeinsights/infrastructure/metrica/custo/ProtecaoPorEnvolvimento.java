package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.projeto.codeinsights.infrastructure.metrica.custo.EnvolventesDaChamada.Protecao;
import com.projeto.codeinsights.infrastructure.metrica.custo.UsoDeCache.Forma;

/**
 * A consulta ao cache protege as auto-chamadas por <b>condiciona-las</b>, em vez de retornar antes
 * delas (esse e o caso de {@link ClassificadorDeCache}, a guarda que retorna). As construcoes que
 * condicionam a chamada vem de {@link EnvolventesDaChamada}: o {@code then} de um {@code if}, um ramo
 * de ternario e o que segue um {@code if (...) continue;} no corpo de um laco.
 * <p>
 * Formas, todas exigindo que <b>toda</b> auto-chamada esteja protegida:
 * <ul>
 *   <li>{@link Forma#CALCULA_SE_AUSENTE} e {@link Forma#TERNARIO}: o ramo grava no cache um valor derivado
 *       da recursao, {@code if (memo[n] == 0) memo[n] = f(n - 1) + f(n - 2);} ou
 *       {@code memo[n] != 0 ? memo[n] : (memo[n] = f(n - 1) + f(n - 2))}. A escrita do resultado e a prova
 *       de que o ramo e "calcular o que falta"; nao se exige nada da chave.</li>
 *   <li>{@link Forma#CHECA_ANTES_DE_CHAMAR} e {@link Forma#PULA_NO_LACO}: o ramo so chama,
 *       {@code if (!vis[v]) visita(v);} ou {@code if (vis[v]) continue; visita(v);}. Sem escrita de
 *       resultado a prova e outra, em tres partes: a <b>regra do indice</b> (algum nome da chave
 *       consultada aparece nos argumentos da chamada: o estado consultado e o estado visitado); a consulta
 *       nao e comparacao relacional ({@code <}, {@code >}), que compara dados em vez de testar uma marca;
 *       e o metodo <b>marca</b> o cache ({@link MarcaNoCache}), o que barra a ordenacao que troca celulas.</li>
 * </ul>
 * A consulta tem de ser garantia do ramo (ver {@link CondicaoDeRamo}). Ficam de fora, e superestimam: a
 * chamada na propria condicao por curto-circuito ({@code if (!vis[v] && dfs(v))}) e a consulta feita por
 * um metodo auxiliar.
 */
final class ProtecaoPorEnvolvimento {

    private final String cache;
    private final DerivacaoDaRecursao recursao;
    private final boolean marcaOCache;

    private ProtecaoPorEnvolvimento(MethodDeclaration metodo, String cache, Collection<MethodCallExpr> autoChamadas) {
        this.cache = cache;
        this.recursao = new DerivacaoDaRecursao(metodo, autoChamadas);
        this.marcaOCache = MarcaNoCache.escreve(metodo, cache, recursao);
    }

    /** A forma que protege todas as auto-chamadas, ou {@link Forma#NENHUMA}. */
    static Forma forma(MethodDeclaration metodo, String cache, Collection<MethodCallExpr> autoChamadas) {
        return new ProtecaoPorEnvolvimento(metodo, cache, autoChamadas).formaQueProtegeTodas(autoChamadas);
    }

    /** A mais fraca das formas das chamadas: basta uma que so checa para o conjunto so checar. */
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
            geral = maisFraca(geral, forma.get());
        }
        return geral;
    }

    /** Pela ordem de {@link Forma}: as que gravam o resultado vem antes das que so checam. */
    private static Forma maisFraca(Forma a, Forma b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    /** A construcao mais interna que protege a chamada; a que grava o resultado vence a que so checa. */
    private Optional<Forma> formaQueProtege(MethodCallExpr chamada) {
        List<Forma> formas = EnvolventesDaChamada.de(chamada).stream()
                .map(protecao -> formaDo(protecao, chamada))
                .filter(forma -> forma != Forma.NENHUMA)
                .toList();
        Optional<Forma> gravando = formas.stream().filter(ProtecaoPorEnvolvimento::gravaOResultado).findFirst();
        return gravando.or(() -> formas.stream().findFirst());
    }

    private static boolean gravaOResultado(Forma forma) {
        return forma == Forma.CALCULA_SE_AUSENTE || forma == Forma.TERNARIO;
    }

    private Forma formaDo(Protecao protecao, MethodCallExpr chamada) {
        List<Expression> consultas = protecao.garantias().stream()
                .filter(condicao -> AcessosAoCache.consulta(condicao, cache))
                .toList();
        if (consultas.isEmpty()) {
            return Forma.NENHUMA;
        }
        if (protecao.ramo() != null && gravaResultadoNoCache(protecao.ramo())) {
            return protecao.seGrava();
        }
        return marcaOEstadoChamado(consultas, chamada) ? protecao.seSoChecar() : Forma.NENHUMA;
    }

    // ----- a regra do indice, a consulta que nao e relacional e a marca -----

    private boolean marcaOEstadoChamado(List<Expression> consultas, MethodCallExpr chamada) {
        return marcaOCache && consultas.stream()
                .anyMatch(consulta -> !CondicaoDeRamo.ehRelacional(consulta) && chaveNosArgumentos(consulta, chamada));
    }

    private boolean chaveNosArgumentos(Expression consulta, MethodCallExpr chamada) {
        Set<String> chave = AcessosAoCache.nomesDaChave(consulta, cache);
        return chamada.getArguments().stream()
                .flatMap(argumento -> AcessosAoCache.nomesEm(argumento).stream())
                .anyMatch(chave::contains);
    }

    /** O ramo atribui ao cache, ou faz {@code put}, um valor que vem da recursao. */
    private boolean gravaResultadoNoCache(Node ramo) {
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
