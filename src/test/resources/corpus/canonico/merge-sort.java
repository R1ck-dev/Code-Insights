class Solucao {
    void ordena(int[] v, int inicio, int fim) {
        if (inicio >= fim) return;
        int meio = (inicio + fim) / 2;
        ordena(v, inicio, meio);
        ordena(v, meio + 1, fim);
        intercala(v, inicio, meio, fim);
    }

    private void intercala(int[] v, int inicio, int meio, int fim) {
        int[] temporario = new int[fim - inicio + 1];
        int i = inicio;
        int j = meio + 1;
        int k = 0;
        while (i <= meio && j <= fim) {
            if (v[i] <= v[j]) temporario[k++] = v[i++];
            else temporario[k++] = v[j++];
        }
        while (i <= meio) temporario[k++] = v[i++];
        while (j <= fim) temporario[k++] = v[j++];
        for (int t = 0; t < temporario.length; t++) v[inicio + t] = temporario[t];
    }
}
