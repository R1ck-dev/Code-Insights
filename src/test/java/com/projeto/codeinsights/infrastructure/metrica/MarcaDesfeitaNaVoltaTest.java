package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;

/**
 * Controles da familia 3 (IC-09): backtracking que marca na ida e <b>desmarca na volta</b>, escrito
 * em formas de protecao que a correcao do R-9 vai passar a reconhecer (continue, if que envolve a
 * chamada, ternario, {@code Set}).
 * <p>
 * Reconhecer essas formas como memoizacao e o risco da correcao: a mesma marca que poda a DFS
 * (visita unica, polinomial) e, quando desfeita depois da chamada, so impede repetir um estado no
 * caminho atual, e a busca segue exaustiva. Cada teste aqui e um algoritmo cujo gabarito na
 * literatura e fatorial; o motor acerta porque a desmarcacao na volta tira o papel de memoizacao
 * (o tempo segue a reducao) e nao pode deixar de acertar. O gabarito aceita {@code O(2^n)} e {@code O(n!)}, como no D8 de
 * {@link DefeitosDoMotorDeJavaTest}: o que se guarda e a ausencia de memoizacao falsa, e nao a
 * escolha entre exponencial e fatorial.
 */
class MarcaDesfeitaNaVoltaTest {

    private final JavaParserAnalisadorMetricas analisador = new JavaParserAnalisadorMetricas(List.of(
            new BigOTempoAnalisador(), new EspacoAnalisador(), new CiclomaticaAnalisador()));

    @Test
    @DisplayName("permutacoes com continue e desmarcacao seguem exponenciais")
    void permutacoesComContinue() {
        assertThat(tempo("""
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
                """)).isIn("O(2^n)", "O(n!)");
    }

    @Test
    @DisplayName("N-rainhas com if que envolve a chamada e desmarcacao segue exponencial")
    void nRainhas() {
        assertThat(tempo("""
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
                """)).isIn("O(2^n)", "O(n!)");
    }

    @Test
    @DisplayName("caminhos simples com ternario e desmarcacao seguem exponenciais")
    void caminhosComTernario() {
        assertThat(tempo("""
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
                """)).isIn("O(2^n)", "O(n!)");
    }

    @Test
    @DisplayName("caminhos simples com Set, contains e remove seguem exponenciais")
    void caminhosComSet() {
        assertThat(tempo("""
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
                """)).isIn("O(2^n)", "O(n!)");
    }

    @Test
    @DisplayName("combinacoes com != 1 num if envolvente e usado[j] = 0 na volta seguem exponenciais")
    void combinacoesComDiferenteDeUm() {
        assertThat(tempo("""
                class Solucao {
                    static int comb(int[] usado, int i, int k) {
                        if (k == 0) return 1;
                        int total = 0;
                        for (int j = i; j < usado.length; j++) {
                            if (usado[j] != 1) {
                                usado[j] = 1;
                                total += comb(usado, j + 1, k - 1);
                                usado[j] = 0;
                            }
                        }
                        return total;
                    }
                }
                """)).isEqualTo("O(2^n)");
    }

    @Test
    @DisplayName("combinacoes com toggle na ida e na volta seguem exponenciais")
    void combinacoesComToggle() {
        assertThat(tempo("""
                class Solucao {
                    static int comb(boolean[] usado, int i, int k) {
                        if (k == 0) return 1;
                        int total = 0;
                        for (int j = i; j < usado.length; j++) {
                            if (usado[j]) continue;
                            usado[j] = !usado[j];
                            total += comb(usado, j + 1, k - 1);
                            usado[j] = !usado[j];
                        }
                        return total;
                    }
                }
                """)).isEqualTo("O(2^n)");
    }

    // ---------------------------------------------------------------- apoio

    private String tempo(String fonte) {
        List<ResultadoMetrica> resultados = analisador.analisar(new Resolucao(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), fonte, LinguagemProgramacao.JAVA, 1, null));
        return resultados.stream()
                .filter(resultado -> resultado.getTipo() == TipoMetrica.BIG_O_TEMPO)
                .findFirst()
                .orElseThrow(() -> new AssertionError("o motor nao produziu " + TipoMetrica.BIG_O_TEMPO))
                .getRotulo();
    }
}
