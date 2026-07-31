class Solucao {
    long conta(int[] v, int inicio, int fim) {
        if (inicio >= fim) return 0;
        int meio = (inicio + fim) / 2;
        long total = conta(v, inicio, meio) + conta(v, meio + 1, fim);
        return total + intercala(v, inicio, meio, fim);
    }

    private long intercala(int[] v, int inicio, int meio, int fim) {
        int[] apoio = new int[fim - inicio + 1];
        int i = inicio;
        int j = meio + 1;
        int k = 0;
        long inversoes = 0;
        while (i <= meio && j <= fim) {
            if (v[i] <= v[j]) {
                apoio[k++] = v[i++];
            } else {
                inversoes += meio - i + 1;
                apoio[k++] = v[j++];
            }
        }
        while (i <= meio) apoio[k++] = v[i++];
        while (j <= fim) apoio[k++] = v[j++];
        for (int t = 0; t < apoio.length; t++) v[inicio + t] = apoio[t];
        return inversoes;
    }
}
