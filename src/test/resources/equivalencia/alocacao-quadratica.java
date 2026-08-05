class Solucao {
    int[][] criarGrade(int n) {
        int[][] grade = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                grade[i][j] = 0;
            }
        }
        return grade;
    }
}
