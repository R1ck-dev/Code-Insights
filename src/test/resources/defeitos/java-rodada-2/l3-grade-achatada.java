class Solucao {
    static long soma(int[][] m, int n) {
        long s = 0;
        for (int k = 0; k < n * n; k++) { int lin = k / n, col = k % n; s += m[lin][col]; }
        return s;
    }
}
