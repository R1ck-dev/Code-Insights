import java.util.HashMap;
import java.util.Map;

class Solucao {
    int maisFrequente(int[] v) {
        Map<Integer, Integer> contagem = new HashMap<>();
        for (int x : v) {
            contagem.put(x, contagem.getOrDefault(x, 0) + 1);
        }

        int melhor = v[0];
        for (Map.Entry<Integer, Integer> par : contagem.entrySet()) {
            if (par.getValue() > contagem.get(melhor)) {
                melhor = par.getKey();
            }
        }
        return melhor;
    }
}
