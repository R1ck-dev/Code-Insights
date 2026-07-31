import java.util.PriorityQueue;

class Solucao {
    int menor(int[] v) {
        PriorityQueue<Integer> fila = new PriorityQueue<>();
        for (int x : v) fila.add(x);
        return fila.poll();
    }
}
