int somarVizinhos(int grade[], int n) {
    int total = 0;
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    int linha = i + di;
                    int coluna = j + dj;
                    if (linha >= 0 && linha < n && coluna >= 0 && coluna < n) {
                        total = total + grade[linha * n + coluna];
                    }
                }
            }
        }
    }
    return total;
}
