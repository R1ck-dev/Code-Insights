import java.util.ArrayList;
import java.util.List;
class Solucao {
    boolean balanceado(String s) {
        List<Character> pilha = new ArrayList<>();
        for (char c : s.toCharArray()) {
            if (c == '(') {
                pilha.add(c);
            } else {
                if (pilha.isEmpty()) return false;
                pilha.remove(pilha.size() - 1);
            }
        }
        return pilha.isEmpty();
    }
}
