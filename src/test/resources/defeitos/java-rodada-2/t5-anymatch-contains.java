import java.util.List;
class Solucao {
    boolean temComum(List<Integer> a, List<Integer> b) {
        return a.stream().anyMatch(x -> b.contains(x));
    }
}
