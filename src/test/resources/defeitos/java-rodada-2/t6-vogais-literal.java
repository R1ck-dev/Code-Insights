class Solucao {
    int vogais(String texto) {
        int total = 0;
        for (char c : texto.toCharArray()) {
            if ("aeiouAEIOU".indexOf(c) >= 0) total++;
        }
        return total;
    }
}
