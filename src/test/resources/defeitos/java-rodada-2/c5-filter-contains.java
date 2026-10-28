import java.util.List;
class Solucao {
    long contaComuns(List<Integer> a, List<Integer> b) {
        return a.stream().filter(x -> b.contains(x)).count();
    }
}
