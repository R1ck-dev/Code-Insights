class Solucao {
    int busca(int[] v, int alvo) {
        int inicio = 0;
        int fim = v.length - 1;
        while (inicio <= fim) {
            int meio = inicio + (fim - inicio) / 2;
            if (v[meio] == alvo) return meio;
            if (v[meio] < alvo) inicio = meio + 1;
            else fim = meio - 1;
        }
        return -1;
    }
}
