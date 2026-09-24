import java.util.List;
import java.util.stream.Collectors;
class Solucao {
    List<Integer> pares(List<Integer> numeros) {
        return numeros.stream()
                .filter(x -> x % 2 == 0)
                .collect(Collectors.toList());
    }
}
