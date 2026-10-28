class Solucao {
    boolean busca(char[][] b, String w, int i, int j, int k) {
        if (k == w.length()) return true;
        if (i < 0 || j < 0 || i >= b.length || j >= b[0].length || b[i][j] != w.charAt(k)) return false;
        char tmp = b[i][j];
        b[i][j] = '#';
        boolean achou = busca(b, w, i + 1, j, k + 1) || busca(b, w, i - 1, j, k + 1)
                || busca(b, w, i, j + 1, k + 1) || busca(b, w, i, j - 1, k + 1);
        b[i][j] = tmp;
        return achou;
    }
}
