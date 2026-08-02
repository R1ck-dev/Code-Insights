import java.util.ArrayList;
import java.util.List;

class Solucao {
    List<List<Integer>> grade(int n) {
        List<List<Integer>> g = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Integer> linha = new ArrayList<>();
            for (int j = 0; j < n; j++) linha.add(j);
            g.add(linha);
        }
        return g;
    }
}
