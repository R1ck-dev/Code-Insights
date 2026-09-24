import java.util.List;
class Solucao {
    static int linhas(List<String> palavras) {
        int total = 0;
        for (int i = 0; i < palavras.size(); i++) {
            String linha = String.join(" ", palavras);
            total += linha.length();
        }
        return total;
    }
}
