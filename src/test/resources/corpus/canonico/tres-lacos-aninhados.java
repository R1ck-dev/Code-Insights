class Solucao {
    int m(int n) {
        int s = 0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                for (int k = 0; k < n; k++) s++;
        return s;
    }
}
