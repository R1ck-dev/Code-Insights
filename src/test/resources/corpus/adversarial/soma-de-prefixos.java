class Solucao {
    int[] prefixos(int[] v) {
        int[] soma = new int[v.length + 1];
        for (int i = 0; i < v.length; i++) {
            soma[i + 1] = soma[i] + v[i];
        }
        return soma;
    }

    int intervalo(int[] soma, int inicio, int fim) {
        return soma[fim + 1] - soma[inicio];
    }
}
