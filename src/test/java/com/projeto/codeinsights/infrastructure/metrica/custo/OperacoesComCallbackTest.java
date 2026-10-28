package com.projeto.codeinsights.infrastructure.metrica.custo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;

/**
 * Quantas vezes o callback de cada chamada roda. Os casos dividem-se em dois: os que precisam
 * multiplicar (o lambda de {@code filter}, o comparador de {@code sort}) e os que impedem a regra
 * de inventar custo — {@code computeIfAbsent}, {@code Optional.map} e um {@code filter} do aluno,
 * cujo lambda roda no maximo uma vez ou nem e da biblioteca.
 */
class OperacoesComCallbackTest {

    /** O fator da primeira chamada de nome {@code operacao} no codigo, ou vazio se ela nao repete o callback. */
    private Optional<Custo> repeticoes(String codigo, String operacao) {
        CompilationUnit unidade = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21))
                .parse(codigo).getResult().orElseThrow();
        MethodCallExpr chamada = unidade.findAll(MethodCallExpr.class).stream()
                .filter(candidata -> candidata.getNameAsString().equals(operacao))
                .findFirst().orElseThrow();
        return OperacoesComCallback.repeticoes(chamada, ResolvedorDeChamadas.de(unidade))
                .map(CustoAvaliado::custo);
    }

    private static String metodo(String corpo) {
        return """
                import java.util.*;
                import java.util.stream.*;
                class Solucao {
                    void f(List<Integer> a, Map<Integer, List<Integer>> m, int n) {
                        %s
                    }
                }
                """.formatted(corpo);
    }

    @Nested
    @DisplayName("operacoes que percorrem os elementos")
    class Percorrem {

        @Test
        @DisplayName("filter, forEach e anyMatch de stream rodam o lambda n vezes")
        void operacoesDeStream() {
            assertThat(repeticoes(metodo("a.stream().filter(x -> x > 0).count();"), "filter")).contains(Custo.LINEAR);
            assertThat(repeticoes(metodo("a.stream().forEach(x -> {});"), "forEach")).contains(Custo.LINEAR);
            assertThat(repeticoes(metodo("a.stream().anyMatch(x -> x > 0);"), "anyMatch")).contains(Custo.LINEAR);
        }

        @Test
        @DisplayName("forEach, removeIf e replaceAll de colecao e Map.forEach tambem")
        void operacoesDeColecao() {
            assertThat(repeticoes(metodo("a.forEach(x -> {});"), "forEach")).contains(Custo.LINEAR);
            assertThat(repeticoes(metodo("a.removeIf(x -> x > 0);"), "removeIf")).contains(Custo.LINEAR);
            assertThat(repeticoes(metodo("a.replaceAll(x -> x + 1);"), "replaceAll")).contains(Custo.LINEAR);
            assertThat(repeticoes(metodo("m.forEach((k, v) -> {});"), "forEach")).contains(Custo.LINEAR);
        }

        @Test
        @DisplayName("filter encadeado em map: cada estagio repete")
        void estagiosEncadeados() {
            String codigo = metodo("a.stream().filter(x -> x > 0).map(x -> x + 1).count();");

            assertThat(repeticoes(codigo, "filter")).contains(Custo.LINEAR);
            assertThat(repeticoes(codigo, "map")).contains(Custo.LINEAR);
        }
    }

    @Nested
    @DisplayName("comparador")
    class Comparador {

        @Test
        @DisplayName("sort e sorted comparam n log n vezes")
        void ordenacao() {
            assertThat(repeticoes(metodo("Collections.sort(a, (p, q) -> p - q);"), "sort")).contains(Custo.N_LOG_N);
            assertThat(repeticoes(metodo("a.sort((p, q) -> p - q);"), "sort")).contains(Custo.N_LOG_N);
            assertThat(repeticoes(metodo("Integer[] v = new Integer[n]; Arrays.sort(v, 0, n, null);"), "sort"))
                    .contains(Custo.N_LOG_N);
            assertThat(repeticoes(metodo("a.stream().sorted((p, q) -> p - q).count();"), "sorted"))
                    .contains(Custo.N_LOG_N);
        }

        @Test
        @DisplayName("min e max de stream comparam n vezes")
        void extremos() {
            assertThat(repeticoes(metodo("a.stream().max((p, q) -> p - q);"), "max")).contains(Custo.LINEAR);
            assertThat(repeticoes(metodo("a.stream().min((p, q) -> p - q);"), "min")).contains(Custo.LINEAR);
        }
    }

    @Nested
    @DisplayName("faixa fixa")
    class FaixaFixa {

        @Test
        @DisplayName("range com os dois limites literais repete um numero constante de vezes")
        void limitesLiterais() {
            assertThat(repeticoes(metodo("IntStream.range(0, 5).forEach(i -> {});"), "forEach"))
                    .contains(Custo.CONSTANTE);
            assertThat(repeticoes(metodo("IntStream.rangeClosed(1, 10).filter(i -> i > 2).count();"), "filter"))
                    .contains(Custo.CONSTANTE);
        }

        @Test
        @DisplayName("etapas que nao geram elementos mantem a faixa constante")
        void etapasIntermediarias() {
            assertThat(repeticoes(metodo("IntStream.range(0, 5).filter(i -> i > 1).forEach(i -> {});"), "forEach"))
                    .contains(Custo.CONSTANTE);
        }

        @Test
        @DisplayName("range com um limite variavel repete n vezes")
        void limiteVariavel() {
            assertThat(repeticoes(metodo("IntStream.range(0, n).forEach(i -> {});"), "forEach")).contains(Custo.LINEAR);
            assertThat(repeticoes(metodo("IntStream.range(n, 5).forEach(i -> {});"), "forEach")).contains(Custo.LINEAR);
        }

        @Test
        @DisplayName("flatMap depois do range pode gerar n elementos: a faixa deixa de ser constante")
        void flatMapQuebraAFaixa() {
            String codigo = metodo("IntStream.range(0, 5).boxed().flatMap(i -> a.stream()).forEach(x -> {});");

            assertThat(repeticoes(codigo, "forEach")).contains(Custo.LINEAR);
        }
    }

    @Nested
    @DisplayName("o lambda que roda no maximo uma vez nao repete")
    class RodaUmaVez {

        @Test
        @DisplayName("computeIfAbsent, merge e compute")
        void operacoesDeMap() {
            assertThat(repeticoes(metodo("m.computeIfAbsent(1, k -> new ArrayList<>());"), "computeIfAbsent")).isEmpty();
            assertThat(repeticoes(metodo("m.merge(1, a, (p, q) -> p);"), "merge")).isEmpty();
            assertThat(repeticoes(metodo("m.compute(1, (k, v) -> v);"), "compute")).isEmpty();
        }

        @Test
        @DisplayName("map, filter e ifPresent de Optional")
        void operacoesDeOptional() {
            assertThat(repeticoes(metodo("a.stream().findFirst().map(x -> x + 1);"), "map")).isEmpty();
            assertThat(repeticoes(metodo("Optional.of(n).filter(x -> x > 0);"), "filter")).isEmpty();
            assertThat(repeticoes(metodo("Optional<Integer> o = Optional.empty(); o.map(x -> x + 1);"), "map")).isEmpty();
            assertThat(repeticoes(metodo("Optional.of(n).ifPresent(x -> {});"), "ifPresent")).isEmpty();
        }

        @Test
        @DisplayName("filter de metodo do aluno nao e o filter do Stream")
        void metodoDoAluno() {
            String codigo = """
                    import java.util.*;
                    class Solucao {
                        static class Caixa {
                            Caixa filter(Runnable r) { return this; }
                        }
                        void f(Caixa c) {
                            c.filter(() -> {});
                        }
                    }
                    """;

            assertThat(repeticoes(codigo, "filter")).isEmpty();
        }
    }
}
