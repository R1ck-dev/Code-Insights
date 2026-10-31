package com.projeto.codeinsights.infrastructure.metrica.custo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;

/**
 * Quais nomes a tabela plana de tipos nao consegue resolver com seguranca: os declarados com mais de
 * um tipo distinto na unidade.
 */
class NomesAmbiguosTest {

    private static final String UNIDADE = """
            class A { B outro; B mesmo; Integer n; }
            class C { Z outro; B mesmo; void g() { var criado = new B(); } }
            class D { void f(B b, int n) { var criado = new Z(); } }
            """;

    private static boolean ambiguo(String receptor) {
        CompilationUnit unidade = StaticJavaParser.parse(UNIDADE);
        Expression expressao = StaticJavaParser.parseExpression(receptor);
        return NomesAmbiguos.de(unidade).receptorAmbiguo(expressao);
    }

    @Test
    @DisplayName("mesmo nome com dois tipos distintos e ambiguo")
    void doisTiposDistintos() {
        assertThat(ambiguo("outro")).isTrue();
    }

    @Test
    @DisplayName("mesmo nome repetido com o mesmo tipo nao e ambiguo")
    void mesmoTipoRepetido() {
        assertThat(ambiguo("mesmo")).isFalse();
        assertThat(ambiguo("b")).isFalse();
    }

    @Test
    @DisplayName("var com new conta pelo tipo criado")
    void varComNewContaPeloTipoCriado() {
        assertThat(ambiguo("criado")).isTrue();
    }

    @Test
    @DisplayName("nome nao declarado, this e criacao nao sao ambiguos")
    void semNomeDeclarado() {
        assertThat(ambiguo("desconhecido")).isFalse();
        assertThat(ambiguo("this")).isFalse();
        assertThat(ambiguo("new B()")).isFalse();
    }

    @Test
    @DisplayName("campo, parenteses, indice e cadeia de chamadas herdam a ambiguidade do elo")
    void formasDeReceptor() {
        assertThat(ambiguo("this.outro")).isTrue();
        assertThat(ambiguo("(outro)")).isTrue();
        assertThat(ambiguo("outro[0]")).isTrue();
        assertThat(ambiguo("outro.proximo()")).isTrue();
        assertThat(ambiguo("mesmo.proximo()")).isFalse();
        assertThat(ambiguo("((B) outro)")).isFalse();
    }
}
