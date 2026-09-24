class Solucao {
    static int busca(int[] v, int x, int ini, int fim) {
        int pos = -1;
        if (ini <= fim) {
            int meio = (ini + fim) / 2;
            if (v[meio] == x) pos = meio;
            if (v[meio] < x) pos = busca(v, x, meio + 1, fim);
            if (v[meio] > x) pos = busca(v, x, ini, meio - 1);
        }
        return pos;
    }
}
