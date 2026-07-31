class Solucao {
    int busca(int[] v, int alvo, int inicio, int fim) {
        if (inicio > fim) return -1;
        int meio = inicio + (fim - inicio) / 2;
        if (v[meio] == alvo) return meio;
        if (v[meio] < alvo) return busca(v, alvo, meio + 1, fim);
        return busca(v, alvo, inicio, meio - 1);
    }
}
