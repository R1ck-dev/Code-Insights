package com.projeto.codeinsights.infrastructure.metrica.custo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O nucleo de SCC sobre grafos pequenos escritos a mao. O que importa para o motor: o que e ciclo de
 * dois ou mais nos, o que NAO e (auto-laco, no isolado, cadeia) e a ordem em que os componentes saem.
 */
class ComponentesFortementeConexosTest {

    /** Grafo por lista de adjacencia: {@code arestas[i]} sao os vizinhos do no {@code i}. */
    private static IntFunction<int[]> grafo(int[]... arestas) {
        return no -> arestas[no];
    }

    private static List<List<Integer>> comoListas(List<int[]> componentes) {
        return componentes.stream()
                .map(componente -> java.util.Arrays.stream(componente).boxed().toList())
                .toList();
    }

    @Test
    @DisplayName("grafo vazio nao tem componente algum")
    void grafoVazio() {
        assertThat(ComponentesFortementeConexos.de(0, grafo())).isEmpty();
        assertThat(ComponentesFortementeConexos.ciclicos(0, grafo())).isEmpty();
    }

    @Test
    @DisplayName("no isolado e um componente de um no e nao e ciclo")
    void noIsolado() {
        IntFunction<int[]> g = grafo(new int[] {});
        assertThat(comoListas(ComponentesFortementeConexos.de(1, g))).containsExactly(List.of(0));
        assertThat(ComponentesFortementeConexos.ciclicos(1, g)).isEmpty();
        assertThat(ComponentesFortementeConexos.temAutoLaco(0, g)).isFalse();
    }

    @Test
    @DisplayName("auto-laco: componente de um no, fora de ciclicos, consultavel por temAutoLaco")
    void autoLaco() {
        IntFunction<int[]> g = grafo(new int[] {0});
        assertThat(comoListas(ComponentesFortementeConexos.de(1, g))).containsExactly(List.of(0));
        assertThat(ComponentesFortementeConexos.ciclicos(1, g)).isEmpty();
        assertThat(ComponentesFortementeConexos.temAutoLaco(0, g)).isTrue();
    }

    @Test
    @DisplayName("auto-laco num no que tambem chama outro continua fora de ciclicos")
    void autoLacoComCadeia() {
        IntFunction<int[]> g = grafo(new int[] {0, 1}, new int[] {});
        assertThat(ComponentesFortementeConexos.ciclicos(2, g)).isEmpty();
        assertThat(ComponentesFortementeConexos.temAutoLaco(0, g)).isTrue();
        assertThat(ComponentesFortementeConexos.temAutoLaco(1, g)).isFalse();
    }

    @Test
    @DisplayName("2-ciclo (par/impar) e um componente ciclico")
    void doisCiclo() {
        IntFunction<int[]> g = grafo(new int[] {1}, new int[] {0});
        assertThat(comoListas(ComponentesFortementeConexos.ciclicos(2, g))).containsExactly(List.of(0, 1));
    }

    @Test
    @DisplayName("3-ciclo (a -> b -> c -> a) e um componente so, com os membros em ordem de indice")
    void tresCiclo() {
        IntFunction<int[]> g = grafo(new int[] {1}, new int[] {2}, new int[] {0});
        assertThat(comoListas(ComponentesFortementeConexos.ciclicos(3, g))).containsExactly(List.of(0, 1, 2));
    }

    @Test
    @DisplayName("cadeia sem volta nao e ciclo")
    void cadeiaSemVolta() {
        IntFunction<int[]> g = grafo(new int[] {1}, new int[] {2}, new int[] {});
        assertThat(ComponentesFortementeConexos.ciclicos(3, g)).isEmpty();
        assertThat(comoListas(ComponentesFortementeConexos.de(3, g)))
                .containsExactly(List.of(2), List.of(1), List.of(0));
    }

    @Test
    @DisplayName("dois SCCs ligados saem separados, o chamado antes do chamador")
    void doisSccsLigados() {
        // {0,1} chama {2,3}; sem aresta de volta.
        IntFunction<int[]> g = grafo(new int[] {1}, new int[] {0, 2}, new int[] {3}, new int[] {2});
        assertThat(comoListas(ComponentesFortementeConexos.ciclicos(4, g)))
                .containsExactly(List.of(2, 3), List.of(0, 1));
    }

    @Test
    @DisplayName("a ordem de saida nao depende de onde a busca comeca: chamados primeiro mesmo com indice maior")
    void chamadosPrimeiroComIndiceMaior() {
        // 0 chama 1 e 2; 1 e 2 formam um ciclo. O raiz tem o menor indice e sai por ultimo.
        IntFunction<int[]> g = grafo(new int[] {1}, new int[] {2}, new int[] {1});
        assertThat(comoListas(ComponentesFortementeConexos.de(3, g)))
                .containsExactly(List.of(1, 2), List.of(0));
    }

    @Test
    @DisplayName("ciclo alcancado por um no fora dele nao arrasta o no para o componente")
    void noForaDoCicloNaoEntra() {
        IntFunction<int[]> g = grafo(new int[] {1}, new int[] {2}, new int[] {1});
        assertThat(comoListas(ComponentesFortementeConexos.ciclicos(3, g))).containsExactly(List.of(1, 2));
    }
}
