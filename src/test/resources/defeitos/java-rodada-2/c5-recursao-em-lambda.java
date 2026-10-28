import java.util.ArrayList;
import java.util.List;
class Solucao {
    static class No {
        List<No> filhos = new ArrayList<>();
    }

    static int contar(No no) {
        return 1 + no.filhos.stream().mapToInt(f -> contar(f)).sum();
    }
}
