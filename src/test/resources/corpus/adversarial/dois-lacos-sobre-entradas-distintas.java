class Solucao {
    int pares(int[] a, int[] b) {
        int total = 0;
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < b.length; j++)
                total++;
        return total;
    }
}
