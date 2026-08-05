class Solucao {
    int somarLinha(int[] grade, int inicio, int n) {
        int total = 0;
        for (int j = 0; j < n; j++) {
            total = total + grade[inicio + j];
        }
        return total;
    }

    int somarTudo(int[] grade, int n) {
        int total = 0;
        for (int i = 0; i < n; i++) {
            total = total + somarLinha(grade, i * n, n);
        }
        return total;
    }
}
