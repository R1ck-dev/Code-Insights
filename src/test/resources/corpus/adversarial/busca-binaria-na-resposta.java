class Solucao {
    int menorCapacidade(int[] pesos, int dias) {
        int menor = 0;
        int maior = 0;
        for (int p : pesos) {
            if (p > menor) menor = p;
            maior += p;
        }

        while (menor < maior) {
            int meio = menor + (maior - menor) / 2;
            if (cabeEm(pesos, meio, dias)) maior = meio;
            else menor = meio + 1;
        }
        return menor;
    }

    private boolean cabeEm(int[] pesos, int capacidade, int dias) {
        int usados = 1;
        int atual = 0;
        for (int p : pesos) {
            if (atual + p > capacidade) {
                usados++;
                atual = 0;
            }
            atual += p;
        }
        return usados <= dias;
    }
}
