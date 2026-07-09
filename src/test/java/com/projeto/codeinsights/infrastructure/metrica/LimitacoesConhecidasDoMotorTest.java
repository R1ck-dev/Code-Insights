package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;

/**
 * Casos em que o motor <b>sabidamente</b> nao acerta a classe exata. Documenta-los como
 * teste, e nao como comentario, tem dois efeitos: torna a imprecisao explicita para quem
 * for interpretar os dados da pesquisa, e faz qualquer mudanca nesse comportamento
 * aparecer como um teste vermelho, em vez de passar despercebida.
 * <p>
 * Nenhum destes casos e um bug a corrigir "na proxima": cada um exigiria uma analise
 * estritamente mais forte (analise de fluxo de dados, ou contagem simbolica de estados)
 * do que a que o motor faz hoje.
 */
class LimitacoesConhecidasDoMotorTest {

    private final BigOTempoAnalisador tempo = new BigOTempoAnalisador();

    private MetricaCalculada analisar(String codigo) {
        return tempo.analisar(AnalisadorTestSupport.parse(codigo));
    }

    /**
     * Correto: O(n! * n). O motor ve "auto-chamada dentro de laco" e responde exponencial.
     * Distinguir n! de 2^n exigiria contar o espaco de estados que a recursao percorre.
     * <p>
     * Este caso tambem prende um falso positivo do detector de memoizacao: a guarda
     * {@code if (k == v.length) return;} le {@code v}, e o corpo escreve {@code v[k]} - se a
     * guarda nao precisasse <b>indexar</b> o cache, esta busca exponencial seria lida como
     * recursao memoizada e classificada como O(n).
     */
    @Test
    void backtrackingDePermutacoesEhClassificadoComoExponencialENaoFatorial() {
        MetricaCalculada resultado = analisar("""
                class Solucao {
                    void permuta(int[] v, int k) {
                        if (k == v.length) return;
                        for (int i = k; i < v.length; i++) {
                            int t = v[k];
                            v[k] = v[i];
                            v[i] = t;
                            permuta(v, k + 1);
                            t = v[k];
                            v[k] = v[i];
                            v[i] = t;
                        }
                    }
                }
                """);

        assertThat(resultado.rotulo()).isEqualTo("O(2^n)");
        assertThat(resultado.confianca()).isEqualTo(NivelConfianca.MEDIA);
    }

    /**
     * Correto: O(raiz de n). O motor nao relaciona o quadrado do indice com o limite do laco,
     * entao assume n iteracoes. Superestima, que e o lado seguro de errar.
     */
    @Test
    void lacoLimitadoPelaRaizQuadradaEhClassificadoComoLinear() {
        MetricaCalculada resultado = analisar("""
                class Solucao {
                    boolean primo(int n) {
                        for (int i = 2; i * i <= n; i++) {
                            if (n % i == 0) return false;
                        }
                        return true;
                    }
                }
                """);

        assertThat(resultado.rotulo()).isEqualTo("O(n)");
    }

    /**
     * Correto: O(n * m). O motor tem um unico simbolo para "tamanho da entrada", entao dois
     * lacos sobre colecoes distintas viram n^2. Separa-los exigiria analise de fluxo de dados.
     */
    @Test
    void doisLacosSobreEntradasDistintasViramQuadratico() {
        MetricaCalculada resultado = analisar("""
                class Solucao {
                    int pares(int[] a, int[] b) {
                        int total = 0;
                        for (int i = 0; i < a.length; i++)
                            for (int j = 0; j < b.length; j++)
                                total++;
                        return total;
                    }
                }
                """);

        assertThat(resultado.rotulo()).isEqualTo("O(n^2)");
    }
}
