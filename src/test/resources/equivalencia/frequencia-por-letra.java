class Solucao {
    int letrasDistintas(int[] letras, int n) {
        int[] contagem = new int[26];
        for (int i = 0; i < n; i++) {
            contagem[letras[i] - 'a']++;
        }
        int distintas = 0;
        for (char c = 'a'; c <= 'z'; c++) {
            if (contagem[c - 'a'] > 0) {
                distintas++;
            }
        }
        return distintas;
    }
}
