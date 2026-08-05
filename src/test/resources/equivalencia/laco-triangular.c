int contarIguais(int valores[], int n) {
    int pares = 0;
    for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
            if (valores[i] == valores[j]) {
                pares++;
            }
        }
    }
    return pares;
}
