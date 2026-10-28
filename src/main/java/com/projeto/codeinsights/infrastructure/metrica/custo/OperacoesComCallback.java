package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

/**
 * Quantas vezes o <b>callback</b> de uma chamada de biblioteca roda — o que decide se o custo do
 * corpo do lambda se <b>multiplica</b> ou apenas se soma ao da chamada.
 * <p>
 * {@code a.stream().filter(x -> b.contains(x))} e o laco {@code for (x : a) if (b.contains(x))}
 * com outra sintaxe: o corpo roda uma vez por elemento. Somar os dois custos (o que o avaliador
 * fazia com qualquer argumento) deixava o stream um grau abaixo do laco equivalente. Mas nem todo
 * lambda itera: o de {@code computeIfAbsent}, {@code merge} ou {@code Optional.map} roda no
 * maximo uma vez, e multiplica-lo inventaria custo. Por isso a regra e uma <b>lista explicita</b>
 * das operacoes que percorrem os elementos; o resto mantem o comportamento de antes.
 * <ul>
 *   <li>percorrem os elementos ({@code filter}, {@code map}, {@code forEach}, {@code removeIf}...):
 *       n vezes, pelo pior caso — inclusive o curto-circuito de {@code anyMatch};</li>
 *   <li>ordenam com comparador ({@code sort}, {@code sorted}): n log n comparacoes;</li>
 *   <li>{@code min}/{@code max} com comparador: n comparacoes;</li>
 *   <li>origem em {@code IntStream.range(0, 5)}, com os dois limites literais: a faixa nao cresce
 *       com a entrada, e o corpo repete um numero constante de vezes.</li>
 * </ul>
 */
final class OperacoesComCallback {

    private static final Set<String> PERCORREM = Set.of(
            "filter", "map", "mapToInt", "mapToObj", "mapToLong", "mapToDouble", "flatMap", "peek",
            "forEach", "forEachOrdered", "anyMatch", "allMatch", "noneMatch", "reduce",
            "takeWhile", "dropWhile", "removeIf", "replaceAll");

    private static final Set<String> ORDENAM = Set.of("sort", "sorted");

    private static final Set<String> EXTREMOS = Set.of("min", "max");

    /** {@code Optional} tambem tem {@code map}, {@code filter} e {@code flatMap}, mas o lambda roda uma vez. */
    private static final Set<String> TAMBEM_DO_OPTIONAL = Set.of("filter", "map", "flatMap");

    private static final Set<String> TIPOS_DE_STREAM_NUMERICO = Set.of("IntStream", "LongStream");

    private static final Set<String> FONTES_DE_STREAM = Set.of(
            "stream", "parallelStream", "chars", "codePoints", "lines", "iterate", "generate");

    /** Etapas intermediarias que mantem a faixa constante: nao geram mais elementos do que recebem. */
    private static final Set<String> NAO_CRESCEM = Set.of(
            "filter", "map", "mapToInt", "mapToObj", "mapToLong", "mapToDouble", "boxed", "peek",
            "sorted", "distinct", "limit", "skip", "takeWhile", "dropWhile",
            "asLongStream", "asDoubleStream", "parallel", "sequential", "unordered");

    private static final Set<String> RETORNAM_OPTIONAL = Set.of("findFirst", "findAny", "min", "max");

    private enum Origem { FAIXA_FIXA, OPTIONAL, OUTRA }

    private OperacoesComCallback() {
    }

    /**
     * Quantas vezes o callback da chamada roda, ou vazio quando a chamada nao e uma operacao
     * da biblioteca que o repete (inclusive quando o nome coincide com um metodo da unidade).
     */
    static Optional<CustoAvaliado> repeticoes(MethodCallExpr chamada, ResolvedorDeChamadas resolvedor) {
        if (chamada.getScope().isEmpty() || resolvedor.alvo(chamada).isPresent()) {
            return Optional.empty();
        }
        return fatorDa(chamada.getNameAsString()).flatMap(fator -> ajustarPelaOrigem(chamada, fator, resolvedor));
    }

    private static Optional<Custo> fatorDa(String operacao) {
        if (ORDENAM.contains(operacao)) {
            return Optional.of(Custo.N_LOG_N);
        }
        return PERCORREM.contains(operacao) || EXTREMOS.contains(operacao)
                ? Optional.of(Custo.LINEAR)
                : Optional.empty();
    }

    private static Optional<CustoAvaliado> ajustarPelaOrigem(MethodCallExpr chamada, Custo fator,
            ResolvedorDeChamadas resolvedor) {
        Origem origem = origemDe(chamada.getScope().orElseThrow(), resolvedor.tipos());
        if (origem == Origem.OPTIONAL && TAMBEM_DO_OPTIONAL.contains(chamada.getNameAsString())) {
            return Optional.empty();
        }
        return Optional.of(CustoAvaliado.exato(origem == Origem.FAIXA_FIXA ? Custo.CONSTANTE : fator));
    }

    /** Sobe a cadeia de escopo ate achar de onde os elementos vem. */
    private static Origem origemDe(Expression escopo, TiposDeVariavel tipos) {
        boolean faixaIntacta = true;
        Expression atual = escopo;
        while (atual instanceof MethodCallExpr etapa) {
            if (devolveOptional(etapa)) {
                return Origem.OPTIONAL;
            }
            if (ehFaixa(etapa)) {
                return faixaIntacta && temLimitesLiterais(etapa) ? Origem.FAIXA_FIXA : Origem.OUTRA;
            }
            if (FONTES_DE_STREAM.contains(etapa.getNameAsString())) {
                return Origem.OUTRA;
            }
            faixaIntacta &= NAO_CRESCEM.contains(etapa.getNameAsString());
            atual = etapa.getScope().orElse(null);
        }
        return ehOptionalDeclarado(atual, tipos) ? Origem.OPTIONAL : Origem.OUTRA;
    }

    /** {@code findFirst()}, {@code max(cmp)} e {@code Optional.of(x)} dao um Optional; {@code reduce(op)}, tambem. */
    private static boolean devolveOptional(MethodCallExpr etapa) {
        String nome = etapa.getNameAsString();
        if (RETORNAM_OPTIONAL.contains(nome)) {
            return true;
        }
        if (nome.equals("reduce")) {
            return etapa.getArguments().size() == 1;
        }
        return etapa.getScope().filter(escopo -> ehNomeComecandoCom(escopo, "Optional")).isPresent();
    }

    private static boolean ehFaixa(MethodCallExpr etapa) {
        String nome = etapa.getNameAsString();
        return (nome.equals("range") || nome.equals("rangeClosed"))
                && etapa.getScope().filter(OperacoesComCallback::ehStreamNumerico).isPresent();
    }

    private static boolean ehStreamNumerico(Expression escopo) {
        return escopo instanceof NameExpr nome && TIPOS_DE_STREAM_NUMERICO.contains(nome.getNameAsString());
    }

    /** {@code range(0, 5)} nao depende da entrada; {@code range(0, n)} depende. */
    private static boolean temLimitesLiterais(MethodCallExpr faixa) {
        return faixa.getArguments().size() == 2
                && faixa.getArguments().stream().noneMatch(AvaliadorDeCusto::dependeDaEntrada);
    }

    private static boolean ehOptionalDeclarado(Expression expressao, TiposDeVariavel tipos) {
        String nome = null;
        if (expressao instanceof NameExpr referencia) {
            nome = referencia.getNameAsString();
        } else if (expressao instanceof FieldAccessExpr campo) {
            nome = campo.getNameAsString();
        }
        String tipo = nome == null ? null : tipos.tipoDe(nome);
        return tipo != null && tipo.startsWith("Optional");
    }

    private static boolean ehNomeComecandoCom(Expression expressao, String prefixo) {
        return expressao instanceof NameExpr nome && nome.getNameAsString().startsWith(prefixo);
    }
}
