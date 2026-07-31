import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class Solucao {
    void ordenaPorSegundoCampo(List<int[]> intervalos) {
        Collections.sort(intervalos, new Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                return Integer.compare(a[1], b[1]);
            }
        });
    }
}
