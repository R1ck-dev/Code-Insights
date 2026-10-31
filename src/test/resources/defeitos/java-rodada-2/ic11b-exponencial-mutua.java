class Solucao {
    long a(int n) {
        if (n <= 0) return 1;
        return b(n - 1) + b(n - 1);
    }

    long b(int n) {
        if (n <= 0) return 1;
        return a(n - 1);
    }
}
