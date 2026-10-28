package com.projeto.codeinsights.infrastructure.metrica.custo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;

/**
 * A referencia a metodo ({@code Main::f}) entra no custo como o lambda {@code x -> f(x)} entraria: o
 * metodo da unidade e custeado e, numa operacao que percorre os elementos, multiplicado por n. Os casos
 * dividem-se em dois: os que exigem o custo do metodo apontado ({@code Classe::m}, {@code this::m},
 * {@code receptor::m}) e os que impedem a regra de inventar custo — biblioteca, {@code Optional.map},
 * construtor — ou de fingir saber o que nao sabe (sobrecarga, auto-referencia, ciclo).
 */
class ReferenciaAMetodoTest {

    private static final String LINEAR_ESTATICO = """
            static boolean temNegativo(int[] v) {
                for (int i = 0; i < v.length; i++) if (v[i] < 0) return true;
                return false;
            }
            """;

    private static CompilationUnit parse(String codigo) {
        return new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21))
                .parse(codigo).getResult().orElseThrow();
    }

    private static CustoAvaliado avaliar(String codigo) {
        return AvaliadorDeCusto.doPrograma(parse(codigo));
    }

    private static String classe(String membros) {
        return """
                import java.util.*;
                import java.util.stream.*;
                class Main {
                    %s
                }
                """.formatted(membros);
    }

    @Test
    @DisplayName("Classe::m numa operacao que percorre: custo do metodo x n")
    void referenciaEstatica() {
        CustoAvaliado custo = avaliar(classe(LINEAR_ESTATICO + """
                static long conta(List<int[]> listas) {
                    return listas.stream().filter(Main::temNegativo).count();
                }
                """));
        assertThat(custo.custo()).isEqualTo(Custo.QUADRATICO);
    }

    @Test
    @DisplayName("this::m resolve para o metodo da propria classe")
    void referenciaComThis() {
        CustoAvaliado custo = avaliar(classe("""
                boolean temNegativo(int[] v) {
                    for (int i = 0; i < v.length; i++) if (v[i] < 0) return true;
                    return false;
                }
                long conta(List<int[]> listas) {
                    return listas.stream().filter(this::temNegativo).count();
                }
                """));
        assertThat(custo.custo()).isEqualTo(Custo.QUADRATICO);
    }

    @Test
    @DisplayName("receptor::m com receptor de classe da unidade resolve pelo tipo do receptor")
    void referenciaComReceptor() {
        CustoAvaliado custo = avaliar("""
                import java.util.*;
                import java.util.stream.*;
                class Filtro {
                    boolean passa(String s) {
                        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == 'x') return true;
                        return false;
                    }
                }
                class Main {
                    static long conta(List<String> a, Filtro filtro) {
                        return a.stream().filter(filtro::passa).count();
                    }
                }
                """);
        assertThat(custo.custo()).isEqualTo(Custo.QUADRATICO);
    }

    @Test
    @DisplayName("referencia de biblioteca e O(1) por elemento: nao multiplica")
    void referenciaDeBiblioteca() {
        CustoAvaliado custo = avaliar(classe("""
                static long soma(List<Integer> a) {
                    return a.stream().map(String::valueOf).map(Integer::valueOf).filter(Objects::nonNull).count();
                }
                static void imprime(List<Integer> a) {
                    a.forEach(System.out::println);
                }
                """));
        assertThat(custo.custo()).isEqualTo(Custo.LINEAR);
    }

    @Test
    @DisplayName("Optional.map(Main::f) roda f uma vez: soma, nao multiplica")
    void referenciaForaDeOperacaoQueItera() {
        CustoAvaliado custo = avaliar(classe(LINEAR_ESTATICO + """
                static Optional<Boolean> consulta(Optional<int[]> o) {
                    return o.map(Main::temNegativo);
                }
                """));
        assertThat(custo.custo()).isEqualTo(Custo.LINEAR);
    }

    @Test
    @DisplayName("faixa de limites literais nao multiplica a referencia: um metodo linear 5 vezes segue linear")
    void referenciaEmFaixaFixa() {
        CustoAvaliado custo = avaliar(classe(LINEAR_ESTATICO + """
                static long f(int[] v) {
                    return IntStream.range(0, 5).filter(i -> Main.temNegativo(v)).count();
                }
                static long g(int[] v) {
                    return IntStream.range(0, 5).mapToObj(i -> v).filter(Main::temNegativo).count();
                }
                """));
        assertThat(custo.custo()).isEqualTo(Custo.LINEAR);
    }

    @Test
    @DisplayName("sem main, o metodo so referenciado nao e raiz: entra pelo custo da referencia")
    void metodoSoReferenciadoNaoEhPontoDeEntrada() {
        CompilationUnit unidade = parse(classe(LINEAR_ESTATICO + """
                static long conta(List<int[]> listas) {
                    return listas.stream().filter(Main::temNegativo).count();
                }
                """));
        MethodDeclaration conta = unidade.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.getNameAsString().equals("conta")).findFirst().orElseThrow();
        ResolvedorDeChamadas resolvedor = ResolvedorDeChamadas.de(unidade);

        assertThat(resolvedor.referenciadosPor(conta))
                .extracting(MethodDeclaration::getNameAsString).containsExactly("temNegativo");
        assertThat(resolvedor.chamadosPor(conta)).isEmpty();
        assertThat(AvaliadorDeCusto.doPrograma(unidade).custo()).isEqualTo(Custo.QUADRATICO);
    }

    @Test
    @DisplayName("sobrecargas: vale a de maior custo, com nota e confianca MEDIA (e uma suposicao)")
    void referenciaComSobrecarga() {
        CustoAvaliado custo = avaliar(classe("""
                static boolean eh(int x) {
                    return x > 0;
                }
                static boolean eh(int[] v) {
                    for (int i = 0; i < v.length; i++) if (v[i] < 0) return true;
                    return false;
                }
                static long conta(List<Integer> a) {
                    return a.stream().filter(Main::eh).count();
                }
                """));
        assertThat(custo.custo()).isEqualTo(Custo.QUADRATICO);
        assertThat(custo.notas()).anyMatch(nota -> nota.contains("2 sobrecargas"));
        assertThat(custo.confianca()).isEqualTo(NivelConfianca.MEDIA);
    }

    @Test
    @DisplayName("referencia ao proprio metodo e recursao por callback: sai '?', nunca uma classe inventada")
    void referenciaAoProprioMetodo() {
        CustoAvaliado custo = avaliar(classe("""
                static void percorre(List<Integer> a) {
                    a.forEach(Main::percorre2);
                }
                static void percorre2(Object o) {
                    List.of(o).forEach(Main::percorre2);
                }
                """));
        assertThat(custo.custo().ehDesconhecido()).isTrue();
        assertThat(custo.notas()).anyMatch(nota -> nota.contains("proprio metodo"));
    }

    @Test
    @DisplayName("ciclo por referencia entre dois metodos: '?'")
    void cicloPorReferencia() {
        CustoAvaliado custo = avaliar(classe("""
                static void ida(List<Integer> a) {
                    a.forEach(Main::volta);
                }
                static void volta(Object o) {
                    ida(List.of());
                }
                """));
        assertThat(custo.custo().ehDesconhecido()).isTrue();
    }

    @Test
    @DisplayName("Tipo::new e receptor de tipo desconhecido ficam O(1) por elemento")
    void construtorEReceptorDesconhecido() {
        CustoAvaliado custo = avaliar(classe("""
                static long f(List<Integer> a, Object desconhecido) {
                    return a.stream().map(StringBuilder::new).map(desconhecido::equals).count();
                }
                """));
        assertThat(custo.custo()).isEqualTo(Custo.LINEAR);
    }
}
