class Solucao {
    static void desmarca(boolean[] usado, int j) {
        usado[j] = false;
    }

    static int comb(boolean[] usado, int i, int k) {
        if (k == 0) return 1;
        int total = 0;
        for (int j = i; j < usado.length; j++) {
            if (usado[j]) continue;
            usado[j] = true;
            total += comb(usado, j + 1, k - 1);
            desmarca(usado, j);
        }
        return total;
    }
}
