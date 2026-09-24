class Solucao {
    int numIlhas(char[][] grade) {
        int ilhas = 0;
        for (int i = 0; i < grade.length; i++)
            for (int j = 0; j < grade[0].length; j++)
                if (grade[i][j] == '1') {
                    ilhas++;
                    afunda(grade, i, j);
                }
        return ilhas;
    }
    void afunda(char[][] grade, int i, int j) {
        grade[i][j] = '0';
        if (i > 0 && grade[i - 1][j] == '1') afunda(grade, i - 1, j);
        if (i + 1 < grade.length && grade[i + 1][j] == '1') afunda(grade, i + 1, j);
        if (j > 0 && grade[i][j - 1] == '1') afunda(grade, i, j - 1);
        if (j + 1 < grade[0].length && grade[i][j + 1] == '1') afunda(grade, i, j + 1);
    }
}
