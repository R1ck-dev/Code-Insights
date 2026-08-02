class Solucao {
    int melhor(int[] peso, int[] valor, int capacidade) {
        int[][] dp = new int[peso.length + 1][capacidade + 1];
        for (int i = 1; i <= peso.length; i++) {
            for (int c = 0; c <= capacidade; c++) {
                dp[i][c] = dp[i - 1][c];
                if (peso[i - 1] <= c) {
                    dp[i][c] = Math.max(dp[i][c], dp[i - 1][c - peso[i - 1]] + valor[i - 1]);
                }
            }
        }
        return dp[peso.length][capacidade];
    }
}
