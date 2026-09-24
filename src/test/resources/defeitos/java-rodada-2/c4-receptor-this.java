import java.util.ArrayList;
import java.util.List;
class Turma {
    private List<String> nomes = new ArrayList<>();
    int contarRepetidos(String[] entrada) {
        int repetidos = 0;
        for (String nome : entrada) {
            if (this.nomes.contains(nome)) repetidos++;
            else this.nomes.add(nome);
        }
        return repetidos;
    }
}
