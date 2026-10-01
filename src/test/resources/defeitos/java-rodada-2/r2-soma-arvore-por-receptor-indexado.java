import java.util.*;
class Arvore {
    static class No {
        int valor;
        List<No> filhos = new ArrayList<>();
        int soma() {
            int total = valor;
            for (int i = 0; i < filhos.size(); i++) total += filhos.get(i).soma();
            return total;
        }
    }
}
