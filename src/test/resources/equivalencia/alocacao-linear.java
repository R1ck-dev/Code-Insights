class Solucao {
    int[] copiar(int[] origem, int n) {
        int[] destino = new int[n];
        for (int i = 0; i < n; i++) {
            destino[i] = origem[i];
        }
        return destino;
    }
}
