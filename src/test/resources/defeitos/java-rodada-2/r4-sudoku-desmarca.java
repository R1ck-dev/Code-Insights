class Sudoku {
    boolean resolve(int[][] tab, int pos) {
        int n = tab.length;
        if (pos == n * n) return true;
        int l = pos / n, c = pos % n;
        if (tab[l][c] != 0) return resolve(tab, pos + 1);
        for (int d = 1; d <= n; d++)
            if (pode(tab, l, c, d)) {
                tab[l][c] = d;
                if (resolve(tab, pos + 1)) return true;
                tab[l][c] = 0;
            }
        return false;
    }
    boolean pode(int[][] tab, int l, int c, int d) {
        for (int k = 0; k < tab.length; k++)
            if (tab[l][k] == d || tab[k][c] == d) return false;
        return true;
    }
}
