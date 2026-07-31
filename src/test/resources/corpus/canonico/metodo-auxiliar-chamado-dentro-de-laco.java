class Solucao {
    int soma(int[] v) {
        int total = 0;
        for (int x : v) total += x;
        return total;
    }

    int somaRepetida(int[] v) {
        int total = 0;
        for (int i = 0; i < v.length; i++) total += soma(v);
        return total;
    }
}
