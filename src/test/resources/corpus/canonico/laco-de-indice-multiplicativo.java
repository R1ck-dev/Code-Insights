class Solucao {
    int m(int n) {
        int c = 0;
        for (int i = 1; i < n; i *= 2) c++;
        return c;
    }
}
