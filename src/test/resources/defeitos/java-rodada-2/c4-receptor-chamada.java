import java.util.*;
class Solucao {
    static int arestasRepetidas(List<List<Integer>> adj, int[][] arestas) {
        int r = 0;
        for (int[] e : arestas) if (adj.get(e[0]).contains(e[1])) r++;
        return r;
    }
}
