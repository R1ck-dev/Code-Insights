class Solucao {
    int primosAte(int n) {
        boolean[] composto = new boolean[n + 1];
        int total = 0;
        for (int i = 2; i <= n; i++) {
            if (!composto[i]) {
                total++;
                for (int j = i + i; j <= n; j += i) composto[j] = true;
            }
        }
        return total;
    }
}
