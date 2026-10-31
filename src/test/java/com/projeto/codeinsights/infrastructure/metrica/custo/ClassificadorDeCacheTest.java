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

    // ----- protecao por if envolvente: a consulta nao retorna, envolve a chamada -----

    @Test
    @DisplayName("calcula-se-ausente: if (memo[n] == 0) memo[n] = f(n-1) + f(n-2) e memoizacao")
    void calculaSeAusenteComVetor() {
        assertThat(classificarDaRodada2("r9-fib-calcula-se-ausente.java", "fib"))
                .isEqualTo(new UsoDeCache(Papel.MEMOIZACAO, 1, Forma.CALCULA_SE_AUSENTE));
    }

    @Test
    @DisplayName("calcula-se-ausente com HashMap: if (!memo.containsKey(n)) memo.put(n, f(n-1) + f(n-2))")
    void calculaSeAusenteComMapa() {
        assertThat(classificarDaRodada2("r9-fib-hashmap.java", "fib"))
                .isEqualTo(new UsoDeCache(Papel.MEMOIZACAO, 1, Forma.CALCULA_SE_AUSENTE));
    }

    @Test
    @DisplayName("calcula-se-ausente: o resultado passa por uma local antes de ir para o cache")
    void calculaSeAusenteComLocalResultado() {
        String fonte = """
                class Solucao {
                    static long[] memo = new long[91];

                    static long fib(int n) {
                        if (n < 2) return n;
                        if (memo[n] == 0) {
                            long soma = fib(n - 1) + fib(n - 2);
                            memo[n] = soma;
                        }
                        return memo[n];
                    }
                }
                """;
        assertThat(classificar(fonte, "fib"))
                .isEqualTo(new UsoDeCache(Papel.MEMOIZACAO, 1, Forma.CALCULA_SE_AUSENTE));
    }

    @Test
    @DisplayName("checa-antes: if (a[u][v] == 1 && !visitado[v]) visita(v) e memoizacao, o indice v vai na chamada")
    void checaAntesDeChamarNaDfs() {
        assertThat(classificarDaRodada2("r9-dfs-checa-antes.java", "visita"))
                .isEqualTo(new UsoDeCache(Papel.MEMOIZACAO, 1, Forma.CHECA_ANTES_DE_CHAMAR));
    }

    @Test
    @DisplayName("checa-antes na inundacao: quatro ifs, cada um com a celula consultada nos argumentos")
    void checaAntesNaInundacao() {
        assertThat(classificarDaRodada2("e8-ilhas-testa-antes.java", "afunda"))
                .isEqualTo(new UsoDeCache(Papel.MEMOIZACAO, 2, Forma.CHECA_ANTES_DE_CHAMAR));
    }

    // ----- a protecao precisa valer para todas as chamadas, e a consulta precisa ser da marca -----

    @Test
    @DisplayName("regra do indice: if (v[i] != v[i+1]) { troca; ordena(v); } nao passa o indice i para a chamada")
    void regraDoIndiceBarraOrdenacaoQueReescreve() {
        String fonte = """
                class Solucao {
                    static void ordena(int[] v) {
                        for (int i = 0; i + 1 < v.length; i++)
                            if (v[i] != v[i + 1]) {
                                int t = v[i];
                                v[i] = v[i + 1];
                                v[i + 1] = t;
                                ordena(v);
                            }
                    }
                }
                """;
        assertThat(classificar(fonte, "ordena")).isEqualTo(UsoDeCache.NENHUM);
    }

    @Test
    @DisplayName("consulta relacional compara dados, nao testa marca: if (v[i] > v[i+1]) { troca; ordena(v, i+1); }")
    void consultaRelacionalNaoEhMarca() {
        String fonte = """
                class Solucao {
                    static void ordena(int[] v, int i) {
                        if (i + 1 < v.length && v[i] > v[i + 1]) {
                            int t = v[i];
                            v[i] = v[i + 1];
                            v[i + 1] = t;
                            ordena(v, i + 1);
                        }
                    }
                }
                """;
        assertThat(classificar(fonte, "ordena")).isEqualTo(UsoDeCache.NENHUM);
    }

    @Test
    @DisplayName("uma auto-chamada fora da protecao basta para nao haver memoizacao")
    void todasAsChamadasPrecisamEstarProtegidas() {
        String fonte = """
                class Solucao {
                    static long[] memo = new long[91];

                    static long fib(int n) {
                        if (n < 2) return n;
                        if (memo[n] == 0) memo[n] = fib(n - 1);
                        return memo[n] + fib(n - 2);
                    }
                }
                """;
        assertThat(classificar(fonte, "fib")).isEqualTo(UsoDeCache.NENHUM);
    }

    @Test
    @DisplayName("consulta num ||: o ramo executa mesmo com a marca livre, entao nao protege")
    void consultaNumOuNaoProtege() {
        String fonte = """
                class Grafo {
                    void visita(int[][] adj, boolean[] vis, int u) {
                        vis[u] = true;
                        for (int v = 0; v < adj.length; v++)
                            if (adj[u][v] == 1 || !vis[v]) visita(adj, vis, v);
                    }
                }
                """;
        assertThat(classificar(fonte, "visita")).isEqualTo(UsoDeCache.NENHUM);
    }

    @Test
    @DisplayName("chamada na propria condicao por curto-circuito nao e protegida (superestima, documentado)")
    void chamadaNaCondicaoNaoEhProtegida() {
        String fonte = """
                class Grafo {
                    boolean acha(int[][] adj, boolean[] vis, int u) {
                        vis[u] = true;
                        boolean achou = false;
                        for (int v = 0; v < adj.length; v++)
                            if (adj[u][v] == 1 && !vis[v] && acha(adj, vis, v)) achou = true;
                        return achou;
                    }
                }
                """;
        assertThat(classificar(fonte, "acha")).isEqualTo(UsoDeCache.NENHUM);
    }

    // ----- a protecao passa, e quem rejeita a memoizacao e a desmarcacao na volta -----

    @Test
    @DisplayName("N-rainhas: as marcas protegem a chamada e sao desfeitas, entao e marca desfeita, nao falta de guarda")
    void nRainhasPassaPelaProtecaoEEhDesfeita() {
        String fonte = """
                class Solucao {
                    static int n;
                    static boolean[] col, d1, d2;

                    static int resolve(int l) {
                        if (l == n) return 1;
                        int total = 0;
                        for (int c = 0; c < n; c++)
                            if (!col[c] && !d1[l + c] && !d2[l - c + n - 1]) {
                                col[c] = true;
                                d1[l + c] = true;
                                d2[l - c + n - 1] = true;
                                total += resolve(l + 1);
                                col[c] = false;
                                d1[l + c] = false;
                                d2[l - c + n - 1] = false;
                            }
                        return total;
                    }
                }
                """;
        assertThat(classificar(fonte, "resolve"))
                .isEqualTo(new UsoDeCache(Papel.MARCA_DESFEITA, 1, Forma.CHECA_ANTES_DE_CHAMAR));
    }

    @Test
    @DisplayName("combinacoes com Map: getOrDefault protege a chamada e put(j, false) desfaz, marca desfeita")
    void combinacoesComMapaPassaPelaProtecaoEEhDesfeita() {
        assertThat(classificarDaRodada2("r4-combinacoes-mapa.java", "comb"))
                .isEqualTo(new UsoDeCache(Papel.MARCA_DESFEITA, 1, Forma.CHECA_ANTES_DE_CHAMAR));
    }

    // ----- ternario: cada ramo e protegido pela condicao que o seleciona -----

    @Test
    @DisplayName("ternario: memo[n] != 0 ? memo[n] : (memo[n] = f(n-1) + f(n-2)) e memoizacao, o else grava o resultado")
    void ternarioQueGravaOResultado() {
        assertThat(classificarDaRodada2("r9-fib-ternario.java", "fib"))
                .isEqualTo(new UsoDeCache(Papel.MEMOIZACAO, 1, Forma.TERNARIO));
    }

    @Test
    @DisplayName("ternario com o resultado no ramo then: memo[n] == 0 ? (memo[n] = f(n-1) + f(n-2)) : memo[n]")
    void ternarioComOResultadoNoRamoEntao() {
        String fonte = """
                class Solucao {
                    static long[] memo = new long[91];

                    static long fib(int n) {
                        if (n < 2) return n;
                        return memo[n] == 0 ? (memo[n] = fib(n - 1) + fib(n - 2)) : memo[n];
                    }
                }
                """;
        assertThat(classificar(fonte, "fib")).isEqualTo(new UsoDeCache(Papel.MEMOIZACAO, 1, Forma.TERNARIO));
    }

    @Test
    @DisplayName("ternario: o else so enxerga os termos || da condicao, um && nao garante a consulta")
    void ternarioComEENoElseNaoProtege() {
        String fonte = """
                class Solucao {
                    static long[] memo = new long[91];

                    static long fib(int n, boolean cache) {
                        if (n < 2) return n;
                        return cache && memo[n] != 0 ? memo[n] : (memo[n] = fib(n - 1, cache) + fib(n - 2, cache));
                    }
                }
                """;
        assertThat(classificar(fonte, "fib")).isEqualTo(UsoDeCache.NENHUM);
    }

    @Test
    @DisplayName("ternario sem escrita do resultado: a regra do indice decide, e a marca desfeita tira a memoizacao")
    void caminhosComTernarioSaoMarcaDesfeita() {
        String fonte = """
                class Solucao {
                    static int[][] adj;
                    static boolean[] noCaminho;

                    static int caminhos(int u, int destino) {
                        if (u == destino) return 1;
                        noCaminho[u] = true;
                        int total = 0;
                        for (int v = 0; v < adj.length; v++)
                            total += adj[u][v] == 1 && !noCaminho[v] ? caminhos(v, destino) : 0;
                        noCaminho[u] = false;
                        return total;
                    }
                }
                """;
        assertThat(classificar(fonte, "caminhos"))
                .isEqualTo(new UsoDeCache(Papel.MARCA_DESFEITA, 1, Forma.CHECA_ANTES_DE_CHAMAR));
    }

    // ----- continue: o que vem depois de if (consulta) continue; so executa com a consulta falsa -----

    @Test
    @DisplayName("DFS com continue: if (a[u][v] == 0 || visitado[v]) continue; e memoizacao, o indice v vai na chamada")
    void dfsComContinue() {
        assertThat(classificarDaRodada2("r9-dfs-continue.java", "visita"))
                .isEqualTo(new UsoDeCache(Papel.MEMOIZACAO, 1, Forma.PULA_NO_LACO));
    }

    @Test
    @DisplayName("combinacoes com continue e desmarcacao: a protecao passa, quem rejeita a memoizacao e a volta")
    void combinacoesComContinueSaoMarcaDesfeita() {
        assertThat(classificarDaRodada2("r4-combinacoes-continue.java", "comb"))
                .isEqualTo(new UsoDeCache(Papel.MARCA_DESFEITA, 1, Forma.PULA_NO_LACO));
    }

    @Test
    @DisplayName("caminhos simples com Set, contains/continue e remove na volta: marca desfeita")
    void caminhosComSetEContinueSaoMarcaDesfeita() {
        String fonte = """
                import java.util.Set;

                class Solucao {
                    static int caminhos(int[][] adj, Set<Integer> noCaminho, int u, int destino) {
                        if (u == destino) return 1;
                        noCaminho.add(u);
                        int total = 0;
                        for (int v = 0; v < adj.length; v++) {
                            if (adj[u][v] == 0 || noCaminho.contains(v)) continue;
                            total += caminhos(adj, noCaminho, v, destino);
                        }
                        noCaminho.remove(u);
                        return total;
                    }
                }
                """;
        assertThat(classificar(fonte, "caminhos"))
                .isEqualTo(new UsoDeCache(Papel.MARCA_DESFEITA, 1, Forma.PULA_NO_LACO));
    }

    @Test
    @DisplayName("permutacoes com continue: usado[i] nao aparece em permuta(usado, k + 1), a regra do indice barra")
    void permutacoesComContinueNaoTemIndiceNaChamada() {
        String fonte = """
                class Solucao {
                    static int permuta(boolean[] usado, int k) {
                        if (k == usado.length) return 1;
                        int total = 0;
                        for (int i = 0; i < usado.length; i++) {
                            if (usado[i]) continue;
                            usado[i] = true;
                            total += permuta(usado, k + 1);
                            usado[i] = false;
                        }
                        return total;
                    }
                }
                """;
        assertThat(classificar(fonte, "permuta")).isEqualTo(UsoDeCache.NENHUM);
    }

    @Test
    @DisplayName("continue numa condicao com &&: o corpo executa mesmo com a consulta falsa, nao protege")
    void continueComEENaoProtege() {
        String fonte = """
                class Grafo {
                    void visita(int[][] adj, boolean[] vis, int u) {
                        vis[u] = true;
                        for (int v = 0; v < adj.length; v++) {
                            if (adj[u][v] == 0 && vis[v]) continue;
                            visita(adj, vis, v);
                        }
                    }
                }
                """;
        assertThat(classificar(fonte, "visita")).isEqualTo(UsoDeCache.NENHUM);
    }

    @Test
    @DisplayName("continue com rotulo nao e reconhecido: so o continue sem rotulo vale (conservador)")
    void continueComRotuloNaoProtege() {
        String fonte = """
                class Grafo {
                    void visita(int[][] adj, boolean[] vis, int u) {
                        vis[u] = true;
                        externo:
                        for (int v = 0; v < adj.length; v++) {
                            if (vis[v]) continue externo;
                            visita(adj, vis, v);
                        }
                    }
                }
                """;
        assertThat(classificar(fonte, "visita")).isEqualTo(UsoDeCache.NENHUM);
    }

    @Test
    @DisplayName("continue fora do corpo do laco da chamada: a chamada num laco interno nao e protegida")
    void continueDeForaDoLacoInternoNaoProtege() {
        String fonte = """
                class Grafo {
                    void visita(int[][] adj, boolean[] vis, int u) {
                        vis[u] = true;
                        for (int v = 0; v < adj.length; v++) {
                            if (vis[v]) continue;
                            for (int w = 0; w < adj.length; w++) visita(adj, vis, v);
                        }
                    }
                }
                """;
        assertThat(classificar(fonte, "visita")).isEqualTo(UsoDeCache.NENHUM);
    }

    // ----- residuo de risco: a consulta nao e uma marca, o metodo reordena os dados do vetor -----

    @Test
    @DisplayName("ordenacao: if (a[i] != a[j]) { troca; ordena(a, i+1, j); } compara dados e troca celulas, nao e memo")
    void ordenacaoComTrocaSobIfNaoEhMemoizacao() {
        String fonte = """
                class Solucao {
                    static void ordena(int[] a, int i, int j) {
                        if (i >= j) return;
                        if (a[i] != a[j]) {
                            int t = a[i];
                            a[i] = a[j];
                            a[j] = t;
                            ordena(a, i + 1, j);
                        }
                    }
                }
                """;
        assertThat(classificar(fonte, "ordena")).isEqualTo(UsoDeCache.NENHUM);
    }

    @Test
    @DisplayName("ordenacao com continue: if (a[k] == a[k+1]) continue; troca; ordena(a, k+1, j) tambem nao e memo")
    void ordenacaoComTrocaDepoisDeContinueNaoEhMemoizacao() {
        String fonte = """
                class Solucao {
                    static void ordena(int[] a, int i, int j) {
                        if (i >= j) return;
                        for (int k = i; k < j; k++) {
                            if (a[k] == a[k + 1]) continue;
                            int t = a[k];
                            a[k] = a[k + 1];
                            a[k + 1] = t;
                            ordena(a, k + 1, j);
                        }
                    }
                }
                """;
        assertThat(classificar(fonte, "ordena")).isEqualTo(UsoDeCache.NENHUM);
    }

    @Test
    @DisplayName("insercao recursiva que desloca celulas sob if com indice nos argumentos nao e memo")
    void insercaoQueDeslocaCelulasNaoEhMemoizacao() {
        String fonte = """
                class Solucao {
                    static void insere(int[] a, int j, int chave) {
                        if (j >= 0 && a[j] != chave) {
                            a[j + 1] = a[j];
                            insere(a, j - 1, chave);
                        }
                    }
                }
                """;
        assertThat(classificar(fonte, "insere")).isEqualTo(UsoDeCache.NENHUM);
    }

    @Test
    @DisplayName("rotulagem de distancia: dist[v] = dist[u] + 1 calcula um valor, e marca; segue memoizacao")
    void rotulagemDeDistanciaEhMarca() {
        String fonte = """
                class Grafo {
                    void rotula(int[][] adj, int[] dist, int u) {
                        for (int v = 0; v < adj.length; v++)
                            if (adj[u][v] == 1 && dist[v] == -1) {
                                dist[v] = dist[u] + 1;
                                rotula(adj, dist, v);
                            }
                    }
                }
                """;
        assertThat(classificar(fonte, "rotula"))
                .isEqualTo(new UsoDeCache(Papel.MEMOIZACAO, 1, Forma.CHECA_ANTES_DE_CHAMAR));
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
