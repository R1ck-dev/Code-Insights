class Solucao {
    boolean a(int n) {
        if (n == 0) return true;
        return b(n);
    }

    boolean b(int n) {
        if (n == 0) return false;
        return a(n);
    }
}
