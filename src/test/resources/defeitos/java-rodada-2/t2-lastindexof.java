import java.util.ArrayList;
import java.util.List;
class Solucao {
    List<Integer> particoes(String s) {
        List<Integer> tamanhos = new ArrayList<>();
        int inicio = 0, fim = 0;
        for (int i = 0; i < s.length(); i++) {
            fim = Math.max(fim, s.lastIndexOf(s.charAt(i)));
            if (i == fim) {
                tamanhos.add(fim - inicio + 1);
                inicio = i + 1;
            }
        }
        return tamanhos;
    }
}
