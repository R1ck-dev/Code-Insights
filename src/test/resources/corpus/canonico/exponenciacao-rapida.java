class Solucao {
    long potencia(long x, int n) {
        if (n == 0) return 1;
        long meio = potencia(x, n / 2);
        if (n % 2 == 0) return meio * meio;
        return x * meio * meio;
    }
}
