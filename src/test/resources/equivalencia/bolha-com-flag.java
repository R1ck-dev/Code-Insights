class Solucao {
    void ordenar(int[] valores, int n) {
        int passada;
        int trocou = 1;
        for (passada = 0; trocou == 1; passada++) {
            trocou = 0;
            for (int j = 0; j < n - 1; j++) {
                if (valores[j] > valores[j + 1]) {
                    int temporario = valores[j];
                    valores[j] = valores[j + 1];
                    valores[j + 1] = temporario;
                    trocou = 1;
                }
            }
        }
    }
}
