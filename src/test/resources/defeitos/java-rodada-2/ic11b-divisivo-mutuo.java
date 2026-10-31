class Solucao {
    static long esquerda(int n) {
        if (n <= 1) return 1;
        long soma = 0;
        for (int i = 0; i < n; i++) soma += i;
        return soma + direita(n / 2) + direita(n / 2);
    }

    static long direita(int n) {
        if (n <= 1) return 1;
        long soma = 0;
        for (int i = 0; i < n; i++) soma += i;
        return soma + esquerda(n / 2) + esquerda(n / 2);
    }
}
