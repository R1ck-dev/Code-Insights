import java.util.ArrayList;
import java.util.List;

class Arvore {
    static class No {
        int valor;
        List<No> filhos = new ArrayList<>();
    }

    static int somaNo(No no) {
        int total = no.valor;
        for (int i = 0; i < no.filhos.size(); i++) total += somaFilho(no.filhos.get(i));
        return total;
    }

    static int somaFilho(No filho) {
        return somaNo(filho);
    }
}
