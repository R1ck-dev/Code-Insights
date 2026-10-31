class Solucao {
    boolean a(int n) {
        if (n == 0) return true;
        return b(n - 1);
    }

    boolean b(int n) {
        if (n == 0) return false;
        return c(n - 1);
    }

    boolean c(int n) {
        if (n == 0) return false;
        return a(n - 1);
    }
}
