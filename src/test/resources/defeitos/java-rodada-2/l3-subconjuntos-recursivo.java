class Solucao {
    static int contar(int[] v, int n, int alvo) {
        if (n == 0) return alvo == 0 ? 1 : 0;
        return contar(v, n - 1, alvo - v[n - 1]) + contar(v, n - 1, alvo);
    }
}
