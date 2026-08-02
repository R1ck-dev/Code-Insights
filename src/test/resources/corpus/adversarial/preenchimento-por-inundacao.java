class Solucao {
    void preenche(char[][] grade, int linha, int coluna, char alvo, char nova) {
        if (linha < 0 || linha >= grade.length) return;
        if (coluna < 0 || coluna >= grade[0].length) return;
        if (grade[linha][coluna] != alvo) return;

        grade[linha][coluna] = nova;
        preenche(grade, linha + 1, coluna, alvo, nova);
        preenche(grade, linha - 1, coluna, alvo, nova);
        preenche(grade, linha, coluna + 1, alvo, nova);
        preenche(grade, linha, coluna - 1, alvo, nova);
    }
}
