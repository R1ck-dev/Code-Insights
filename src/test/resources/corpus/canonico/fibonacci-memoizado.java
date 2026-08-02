class Solucao {
    private long[] memo = new long[64];

    long fib(int n) {
        if (n < 2) return n;
        if (memo[n] != 0) return memo[n];
        memo[n] = fib(n - 1) + fib(n - 2);
        return memo[n];
    }
}
