class Solucao {
    boolean existeParComSoma(int[] ordenado, int alvo) {
        int esquerda = 0;
        int direita = ordenado.length - 1;
        while (esquerda < direita) {
            int soma = ordenado[esquerda] + ordenado[direita];
            if (soma == alvo) return true;
            if (soma < alvo) esquerda++;
            else direita--;
        }
        return false;
    }
}
