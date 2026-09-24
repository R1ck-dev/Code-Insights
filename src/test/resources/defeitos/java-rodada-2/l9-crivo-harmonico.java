class Solucao {
    static int[] divisores(int n) {
        int[] div = new int[n + 1];
        for (int i = 1; i <= n; i++) for (int j = i; j <= n; j += i) div[j]++;
        return div;
    }
}
