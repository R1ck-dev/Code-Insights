class Solucao {
    String junta(int[] v) {
        String saida = "";
        for (int x : v) {
            saida = saida.isEmpty() ? "" + x : saida + ", " + x;
        }
        return saida;
    }
}
