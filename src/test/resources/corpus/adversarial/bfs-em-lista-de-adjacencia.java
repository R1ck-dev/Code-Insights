import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

class Solucao {
    int[] distancias(List<List<Integer>> vizinhos, int origem) {
        int[] distancia = new int[vizinhos.size()];
        for (int i = 0; i < distancia.length; i++) distancia[i] = -1;

        Queue<Integer> fila = new ArrayDeque<>();
        distancia[origem] = 0;
        fila.add(origem);

        while (!fila.isEmpty()) {
            int atual = fila.poll();
            for (int proximo : vizinhos.get(atual)) {
                if (distancia[proximo] == -1) {
                    distancia[proximo] = distancia[atual] + 1;
                    fila.add(proximo);
                }
            }
        }
        return distancia;
    }
}
