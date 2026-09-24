import java.util.*;
class Arvore {
    static class No { int valor; List<No> filhos = new ArrayList<>(); }
    static int soma(No no) {
        int total = no.valor;
        for (int i = 0; i < no.filhos.size(); i++) total += soma(no.filhos.get(i));
        return total;
    }
}
