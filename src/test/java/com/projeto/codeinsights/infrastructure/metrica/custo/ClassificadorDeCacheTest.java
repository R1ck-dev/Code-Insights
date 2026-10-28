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
 * O papel do cache nos programas do corpus que fixam a deteccao de memoizacao e, em trechos
 * inline, a separacao entre memoizacao e marca desfeita na volta (backtracking).
 * <p>
 * Os tres primeiros do corpus sao a guarda que retorna com o cache por indice (vetor, grade e
 * visitados); o quarto e o contra-exemplo que motivou exigir a consulta pela chave:
 * {@code if (k == v.length) return;} num backtracking que escreve {@code v[k]} nao e memoizacao.
 */
class ClassificadorDeCacheTest {

    private static final UsoDeCache MARCA_DESFEITA_1 = new UsoDeCache(Papel.MARCA_DESFEITA, 1, Forma.GUARDA_QUE_RETORNA);
    private static final UsoDeCache MARCA_DESFEITA_2 = new UsoDeCache(Papel.MARCA_DESFEITA, 2, Forma.GUARDA_QUE_RETORNA);
    private static final UsoDeCache MEMOIZACAO_1 = new UsoDeCache(Papel.MEMOIZACAO, 1, Forma.GUARDA_QUE_RETORNA);
    private static final UsoDeCache MEMOIZACAO_2 = new UsoDeCache(Papel.MEMOIZACAO, 2, Forma.GUARDA_QUE_RETORNA);

    private UsoDeCache classificarDoCorpus(String caminhoNoCorpus, String nomeDoMetodo) {
        return classificar(lerRecurso("/corpus/" + caminhoNoCorpus), nomeDoMetodo);
    }

    private UsoDeCache classificarDaRodada2(String arquivo, String nomeDoMetodo) {
        return classificar(lerRecurso("/defeitos/java-rodada-2/" + arquivo), nomeDoMetodo);
    }

    /** Classifica o metodo de um trecho de codigo-fonte. */
    private UsoDeCache classificar(String fonte, String nomeDoMetodo) {
        CompilationUnit unidade = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21))
                .parse(fonte).getResult().orElseThrow();
        MethodDeclaration metodo = unidade.findAll(MethodDeclaration.class).stream()
                .filter(candidato -> candidato.getNameAsString().equals(nomeDoMetodo))
                .findFirst().orElseThrow();
        ResolvedorDeChamadas resolvedor = ResolvedorDeChamadas.de(unidade);
        List<MethodCallExpr> autoChamadas = metodo.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> resolvedor.ehAutoChamada(chamada, metodo))
                .toList();
        return ClassificadorDeCache.classificar(metodo, autoChamadas);
    }

    private static String lerRecurso(String caminho) {
        try (InputStream entrada = ClassificadorDeCacheTest.class.getResourceAsStream(caminho)) {
            assertThat(entrada).as("fixture %s", caminho).isNotNull();
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException erro) {
            throw new IllegalStateException(erro);
        }
    }

    // ----- memoizacao: a consulta poda e a escrita fica -----

    @Test
    @DisplayName("Fibonacci memoizado: o vetor memo poda a recursao, um indice")
    void fibonacciMemoizado() {
        assertThat(classificarDoCorpus("canonico/fibonacci-memoizado.java", "fib")).isEqualTo(MEMOIZACAO_1);
    }

    @Test
    @DisplayName("preenchimento por inundacao: a propria grade marca as celulas, dois indices")
    void preenchimentoPorInundacao() {
        assertThat(classificarDoCorpus("adversarial/preenchimento-por-inundacao.java", "preenche"))
                .isEqualTo(MEMOIZACAO_2);
    }

    @Test
    @DisplayName("busca em profundidade: visitado e o cache; a matriz de adjacencia nao e consultada na guarda")
    void buscaEmProfundidade() {
        assertThat(classificarDoCorpus("canonico/busca-em-profundidade-com-visitados.java", "visita"))
                .isEqualTo(MEMOIZACAO_1);
    }

    @Test
    @DisplayName("permutacao por troca: a guarda le v.length, nao v[k], entao nao ha cache")
    void permutacaoPorTroca() {
        assertThat(classificarDoCorpus("adversarial/backtracking-de-permutacoes.java", "permuta"))
                .isEqualTo(UsoDeCache.NENHUM);
    }

    // ----- marca desfeita na volta: a consulta poda, mas a escrita e restaurada depois da chamada -----

    @Test
    @DisplayName("caminhos simples: noCaminho[u] = true ... noCaminho[u] = false e marca desfeita")
    void caminhosSimplesDesmarcam() {
        String fonte = """
                class Solucao {
                    static int[][] adj;

                    static int caminhos(int u, int destino, boolean[] noCaminho) {
                        if (u == destino) return 1;
                        if (noCaminho[u]) return 0;
                        noCaminho[u] = true;
                        int total = 0;
                        for (int v = 0; v < adj.length; v++)
                            if (adj[u][v] == 1) total += caminhos(v, destino, noCaminho);
                        noCaminho[u] = false;
                        return total;
                    }
                }
                """;
        assertThat(classificar(fonte, "caminhos")).isEqualTo(MARCA_DESFEITA_1);
    }

    @Test
    @DisplayName("labirinto: vis[x][y] = true ... vis[x][y] = false e marca desfeita de duas dimensoes")
    void labirintoDesmarca() {
        assertThat(classificarDaRodada2("r4-labirinto-desmarca.java", "caminhos")).isEqualTo(MARCA_DESFEITA_2);
    }

    @Test
    @DisplayName("sudoku: tab[l][c] = d ... tab[l][c] = 0 restaura o valor comparado na guarda (!= 0)")
    void sudokuRestauraOValorLivre() {
        assertThat(classificarDaRodada2("r4-sudoku-desmarca.java", "resolve")).isEqualTo(MARCA_DESFEITA_2);
    }

    @Test
    @DisplayName("busca de palavra: b[i][j] = tmp restaura o valor salvo numa local que leu o cache")
    void buscaDePalavraRestauraOValorSalvo() {
        assertThat(classificarDaRodada2("r4-busca-palavra-restaura.java", "busca")).isEqualTo(MARCA_DESFEITA_2);
    }

    @Test
    @DisplayName("conjunto de visitados com remove na volta e marca desfeita")
    void conjuntoComRemove() {
        String fonte = """
                import java.util.Set;

                class Solucao {
                    static int[][] adj;

                    static int caminhos(int u, int destino, Set<Integer> noCaminho) {
                        if (u == destino) return 1;
                        if (noCaminho.contains(u)) return 0;
                        noCaminho.add(u);
                        int total = 0;
                        for (int v = 0; v < adj.length; v++)
                            if (adj[u][v] == 1) total += caminhos(v, destino, noCaminho);
                        noCaminho.remove(u);
                        return total;
                    }
                }
                """;
        assertThat(classificar(fonte, "caminhos")).isEqualTo(MARCA_DESFEITA_1);
    }

    @Test
    @DisplayName("mapa com put(k, true) e put(k, false) na volta e marca desfeita")
    void mapaComPutFalso() {
        String fonte = """
                import java.util.Map;

                class Solucao {
                    static int[][] adj;

                    static int caminhos(int u, int destino, Map<Integer, Boolean> noCaminho) {
                        if (u == destino) return 1;
                        if (noCaminho.getOrDefault(u, false)) return 0;
                        noCaminho.put(u, true);
                        int total = 0;
                        for (int v = 0; v < adj.length; v++)
                            if (adj[u][v] == 1) total += caminhos(v, destino, noCaminho);
                        noCaminho.put(u, false);
                        return total;
                    }
                }
                """;
        assertThat(classificar(fonte, "caminhos")).isEqualTo(MARCA_DESFEITA_1);
    }

    // ----- nao e desmarcacao: a escrita depois da chamada nao restaura o valor livre -----

    @Test
    @DisplayName("DFS de tres cores: cor[u] = 2 na volta e marca definitiva, nao o valor livre (0)")
    void tresCoresSegueMemoizacao() {
        assertThat(classificarDaRodada2("r9-dfs-tres-cores.java", "visita")).isEqualTo(MEMOIZACAO_1);
    }

    @Test
    @DisplayName("memo de jogo: memo[n] = 1 ou 2 sem marca de ida e memoizacao")
    void memoDeJogoSemMarcaDeIda() {
        String fonte = """
                class Jogo {
                    static int[] memo = new int[100];

                    static int vence(int n) {
                        if (n <= 0) return 2;
                        if (memo[n] != 0) return memo[n];
                        if (vence(n - 1) == 2 || vence(n - 2) == 2) memo[n] = 1;
                        else memo[n] = 2;
                        return memo[n];
                    }
                }
                """;
        assertThat(classificar(fonte, "vence")).isEqualTo(MEMOIZACAO_1);
    }

    @Test
    @DisplayName("memo.put(chave, total): o valor gravado vem da recursao, mesmo com marca de ida")
    void putDoResultadoEhMemoizacao() {
        String fonte = """
                import java.util.Map;

                class Solucao {
                    static Map<String, Long> memo;

                    static long conta(int i, int j) {
                        String chave = i + "," + j;
                        if (memo.containsKey(chave)) return memo.get(chave);
                        memo.put(chave, 0L);
                        long total = 0;
                        for (int k = 0; k < i; k++) total += conta(k, j);
                        memo.put(chave, total);
                        return total;
                    }
                }
                """;
        assertThat(classificar(fonte, "conta")).isEqualTo(MEMOIZACAO_1);
    }

    @Test
    @DisplayName("return memo[i][u] = total: o resultado derivado da recursao nunca e o valor livre")
    void atribuicaoDoResultadoNoReturnEhMemoizacao() {
        String fonte = """
                class Solucao {
                    static int[][] memo;
                    static int[][] adj;

                    static int conta(int i, int u) {
                        if (memo[i][u] != 0) return memo[i][u];
                        memo[i][u] = -1;
                        int total = 0;
                        for (int v = 0; v < adj.length; v++)
                            if (adj[u][v] == 1) total += conta(i + 1, v);
                        return memo[i][u] = total;
                    }
                }
                """;
        assertThat(classificar(fonte, "conta")).isEqualTo(MEMOIZACAO_2);
    }

    // ----- escolha entre candidatos -----

    @Test
    @DisplayName("com memoizacao e marca desfeita no mesmo metodo, a memoizacao vence")
    void memoizacaoVenceMarcaDesfeita() {
        String fonte = """
                class Solucao {
                    static long[] memo = new long[100];

                    static long conta(int n, boolean[] usado) {
                        if (memo[n] != 0) return memo[n];
                        if (usado[n]) return 0;
                        usado[n] = true;
                        long total = n < 2 ? 1 : conta(n - 1, usado) + conta(n - 2, usado);
                        usado[n] = false;
                        return memo[n] = total;
                    }
                }
                """;
        assertThat(classificar(fonte, "conta").papel()).isEqualTo(Papel.MEMOIZACAO);
    }
}
