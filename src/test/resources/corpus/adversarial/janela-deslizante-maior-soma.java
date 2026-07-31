class Solucao {
    int maiorSomaDeJanela(int[] v, int k) {
        int soma = 0;
        for (int i = 0; i < k; i++) soma += v[i];

        int melhor = soma;
        for (int i = k; i < v.length; i++) {
            soma = soma + v[i] - v[i - k];
            if (soma > melhor) melhor = soma;
        }
        return melhor;
    }
}
