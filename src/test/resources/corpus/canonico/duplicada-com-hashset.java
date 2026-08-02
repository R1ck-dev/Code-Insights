import java.util.HashSet;
import java.util.Set;

class Solucao {
    boolean temDuplicada(int[] v) {
        Set<Integer> vistos = new HashSet<>();
        for (int x : v) {
            if (vistos.contains(x)) return true;
            vistos.add(x);
        }
        return false;
    }
}
