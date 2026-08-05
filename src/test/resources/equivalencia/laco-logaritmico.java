class Solucao {
    int contarDobros(int n) {
        int passos = 0;
        for (int i = 1; i < n; i = i * 2) {
            passos++;
        }
        return passos;
    }
}
