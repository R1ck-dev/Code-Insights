void ordenarPorSelecao(int valores[], int n) {
    for (int i = 0; i < n - 1; i++) {
        int menor = i;
        for (int j = i + 1; j < n; j++) {
            if (valores[j] < valores[menor]) {
                menor = j;
            }
        }
        if (menor != i) {
            int guarda = valores[i];
            valores[i] = valores[menor];
            valores[menor] = guarda;
        }
    }
}
