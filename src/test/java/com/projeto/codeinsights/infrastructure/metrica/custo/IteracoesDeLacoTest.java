package com.projeto.codeinsights.infrastructure.metrica.custo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.ForStmt;

/**
 * Quando um {@code for} nao depende da entrada. A regra e o contador do cabecalho: nasce constante no
 * init, e comparado com limite constante e avanca no update. Os casos dividem-se em dois: os que exigem
 * a constante (literal, {@code -1}, {@code 'a'}, init por atribuicao, limite a esquerda) e os que impedem
 * a regra de confundir "a condicao tem um literal" com "o laco e limitado" — flag de parada, contador que
 * nao e o comparado, init que nao e constante.
 */
class IteracoesDeLacoTest {

    /** O custo do primeiro {@code for} do metodo {@code f(int n, int trocou)}. */
    private static Custo primeiroFor(String cabecalho) {
        return primeiroForEm("", cabecalho);
    }

    /** Como {@link #primeiroFor}, com {@code membros} declarados na classe antes do metodo. */
    private static Custo primeiroForEm(String membros, String cabecalho) {
        CompilationUnit unidade = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21))
                .parse("""
                        class Solucao {
                            %s
                            void f(int n, int trocou) {
                                %s
                            }
                        }
                        """.formatted(membros, cabecalho)).getResult().orElseThrow();
        ForStmt laco = unidade.findFirst(ForStmt.class).orElseThrow();
        return IteracoesDeLaco.de(laco, ConstantesFinais.de(unidade)).custo();
    }

    @Test
    @DisplayName("contador declarado no cabecalho, limite literal: constante")
    void contadorDeclarado() {
        assertThat(primeiroFor("for (int i = 0; i < 26; i++) { }")).isEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("contador pre-declarado e iniciado por atribuicao: constante")
    void contadorPreDeclarado() {
        assertThat(primeiroFor("int i; for (i = 0; i < 26; i++) { }")).isEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("-1 a 1 e 'a' a 'z' sao constantes")
    void negativoEChar() {
        assertThat(primeiroFor("for (int di = -1; di <= 1; di++) { }")).isEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("for (char c = 'a'; c <= 'z'; c++) { }")).isEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("limite a esquerda da comparacao (26 > i): constante")
    void limiteAEsquerda() {
        assertThat(primeiroFor("for (int i = 0; 26 > i; i++) { }")).isEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("flag de parada nao e o contador: nao e constante")
    void flagNaoEhConstante() {
        assertThat(primeiroFor("for (int passada = 0; trocou == 1; passada++) { trocou = 0; }"))
                .isNotEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("contador que nao e o comparado: for (d = 0; n > 0; d++) divide por 10 e e logaritmico")
    void contadorNaoComparado() {
        assertThat(primeiroFor("for (int d = 0; n > 0; d++) n /= 10;")).isEqualTo(Custo.LOGARITMICO);
    }

    @Test
    @DisplayName("init nao constante ou sem update no cabecalho: nao e constante")
    void semInitConstanteOuSemUpdate() {
        assertThat(primeiroFor("for (int i = n; i < 26; i++) { }")).isNotEqualTo(Custo.CONSTANTE);
        assertThat(primeiroFor("for (int i = 0; i < 26; ) { i++; }")).isNotEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("limite que e campo final com literal: constante")
    void limiteEhCampoFinal() {
        assertThat(primeiroForEm("static final int MAX = 100;", "for (int i = 0; i < MAX; i++) { }"))
                .isEqualTo(Custo.CONSTANTE);
        assertThat(primeiroForEm("final int MAX = 100;", "for (int i = MAX; i > 0; i--) { }"))
                .isEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("limite que e local final com literal: constante")
    void limiteEhLocalFinal() {
        assertThat(primeiroFor("final int N = 100; for (int i = 0; i < N; i++) { }")).isEqualTo(Custo.CONSTANTE);
    }

    @Test
    @DisplayName("campo nao final, final sem literal e final de outro tipo: nao e constante")
    void naoEhConstanteFinal() {
        assertThat(primeiroForEm("static int MAX = 100;", "for (int i = 0; i < MAX; i++) { }"))
                .isEqualTo(Custo.LINEAR);
        assertThat(primeiroForEm("final int MAX;", "for (int i = 0; i < MAX; i++) { }")).isEqualTo(Custo.LINEAR);
        assertThat(primeiroForEm("static final int MAX = 100 - 1;", "for (int i = 0; i < MAX; i++) { }"))
                .isEqualTo(Custo.LINEAR);
        assertThat(primeiroFor("final int N = n; for (int i = 0; i < N; i++) { }")).isEqualTo(Custo.LINEAR);
    }

    @Test
    @DisplayName("parametro ou local nao final com o nome do campo tira a constante (sombra)")
    void sombraTiraAConstante() {
        assertThat(primeiroForEm("static final int n = 100;", "for (int i = 0; i < n; i++) { }"))
                .isEqualTo(Custo.LINEAR);
        assertThat(primeiroForEm("static final int MAX = 100;", "int MAX = n; for (int i = 0; i < MAX; i++) { }"))
                .isEqualTo(Custo.LINEAR);
    }
}
