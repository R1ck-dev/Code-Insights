class Solucao {
    static int comb(int[] usado, int i, int k) {
        if (k == 0) return 1;
        int total = 0;
        for (int j = i; j < usado.length; j++) {
            if (usado[j] == 1) continue;
            usado[j] = 1;
            total += comb(usado, j + 1, k - 1);
            usado[j] = 0;
        }
        return total;
    }
}
