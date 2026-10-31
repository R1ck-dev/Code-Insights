class Solucao {
    boolean par(int n) {
        if (n == 0) return true;
        return impar(n - 1);
    }

    boolean impar(int n) {
        if (n == 0) return false;
        return par(n - 1);
    }
}
