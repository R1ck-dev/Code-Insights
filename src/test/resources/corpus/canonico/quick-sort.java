class Solucao {
    void ordena(int[] v, int inicio, int fim) {
        if (inicio >= fim) return;
        int pivo = particiona(v, inicio, fim);
        ordena(v, inicio, pivo - 1);
        ordena(v, pivo + 1, fim);
    }

    private int particiona(int[] v, int inicio, int fim) {
        int referencia = v[fim];
        int i = inicio - 1;
        for (int j = inicio; j < fim; j++) {
            if (v[j] <= referencia) {
                i++;
                int tmp = v[i];
                v[i] = v[j];
                v[j] = tmp;
            }
        }
        int tmp = v[i + 1];
        v[i + 1] = v[fim];
        v[fim] = tmp;
        return i + 1;
    }
}
