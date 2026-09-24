import java.util.List;
import java.util.Set;
import java.util.TreeSet;
class Solucao {
    int distintos(List<Integer> v) {
        Set<Integer> ordenados = new TreeSet<>(v);
        return ordenados.size();
    }
}
