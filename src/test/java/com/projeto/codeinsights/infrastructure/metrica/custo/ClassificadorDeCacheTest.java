package com.projeto.codeinsights.infrastructure.metrica.custo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.projeto.codeinsights.infrastructure.metrica.custo.UsoDeCache.Forma;
import com.projeto.codeinsights.infrastructure.metrica.custo.UsoDeCache.Papel;

/**
 * O papel do cache nos programas do corpus que fixam a deteccao de memoizacao. Os tres primeiros
 * sao a guarda que retorna com o cache por indice (vetor, grade e visitados); o ultimo e o contra-
 * exemplo que motivou exigir a consulta pela chave: {@code if (k == v.length) return;} num
 * backtracking que escreve {@code v[k]} nao e memoizacao.
 */
class ClassificadorDeCacheTest {

    private UsoDeCache classificar(String caminhoNoCorpus, String nomeDoMetodo) {
        CompilationUnit unidade = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21))
                .parse(lerDoCorpus(caminhoNoCorpus)).getResult().orElseThrow();
        MethodDeclaration metodo = unidade.findAll(MethodDeclaration.class).stream()
                .filter(candidato -> candidato.getNameAsString().equals(nomeDoMetodo))
                .findFirst().orElseThrow();
        ResolvedorDeChamadas resolvedor = ResolvedorDeChamadas.de(unidade);
        List<MethodCallExpr> autoChamadas = metodo.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> resolvedor.ehAutoChamada(chamada, metodo))
                .toList();
        return ClassificadorDeCache.classificar(metodo, autoChamadas);
    }

    private static String lerDoCorpus(String caminho) {
        try (InputStream entrada = ClassificadorDeCacheTest.class.getResourceAsStream("/corpus/" + caminho)) {
            assertThat(entrada).as("fixture do corpus %s", caminho).isNotNull();
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException erro) {
            throw new IllegalStateException(erro);
        }
    }

    @Test
    @DisplayName("Fibonacci memoizado: o vetor memo poda a recursao, um indice")
    void fibonacciMemoizado() {
        assertThat(classificar("canonico/fibonacci-memoizado.java", "fib"))
                .isEqualTo(new UsoDeCache(Papel.MEMOIZACAO, 1, Forma.GUARDA_QUE_RETORNA));
    }

    @Test
    @DisplayName("preenchimento por inundacao: a propria grade marca as celulas, dois indices")
    void preenchimentoPorInundacao() {
        assertThat(classificar("adversarial/preenchimento-por-inundacao.java", "preenche"))
                .isEqualTo(new UsoDeCache(Papel.MEMOIZACAO, 2, Forma.GUARDA_QUE_RETORNA));
    }

    @Test
    @DisplayName("busca em profundidade: visitado e o cache; a matriz de adjacencia nao e consultada na guarda")
    void buscaEmProfundidade() {
        assertThat(classificar("canonico/busca-em-profundidade-com-visitados.java", "visita"))
                .isEqualTo(new UsoDeCache(Papel.MEMOIZACAO, 1, Forma.GUARDA_QUE_RETORNA));
    }

    @Test
    @DisplayName("permutacao por troca: a guarda le v.length, nao v[k], entao nao ha cache")
    void permutacaoPorTroca() {
        assertThat(classificar("adversarial/backtracking-de-permutacoes.java", "permuta"))
                .isEqualTo(UsoDeCache.NENHUM);
    }
}
