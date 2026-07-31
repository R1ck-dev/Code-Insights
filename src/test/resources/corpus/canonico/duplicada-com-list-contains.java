import java.util.ArrayList;
import java.util.List;

class Solucao {
    boolean temDuplicada(int[] v) {
        List<Integer> vistos = new ArrayList<>();
        for (int x : v) {
            if (vistos.contains(x)) return true;
            vistos.add(x);
        }
        return false;
    }
}
