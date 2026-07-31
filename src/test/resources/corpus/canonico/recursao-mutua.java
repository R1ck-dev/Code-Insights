class Solucao {
    boolean par(int n) {
        return n == 0 ? true : impar(n - 1);
    }

    boolean impar(int n) {
        return n == 0 ? false : par(n - 1);
    }
}
