class Solucao {
    int vogais(String texto) {
        int total = 0;
        for (char c : texto.toCharArray()) {
            switch (c) {
                case 'a', 'e', 'i', 'o', 'u' -> total++;
                default -> { }
            }
        }
        return total;
    }
}
