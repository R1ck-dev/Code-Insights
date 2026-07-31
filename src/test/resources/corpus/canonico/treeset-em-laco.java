import java.util.Set;
import java.util.TreeSet;

class Solucao {
    int distintos(int[] v) {
        Set<Integer> ordenados = new TreeSet<>();
        for (int x : v) ordenados.add(x);
        return ordenados.size();
    }
}
