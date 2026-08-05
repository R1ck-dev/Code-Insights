int particionar(int valores[], int inicio, int fim) {
    int pivo = valores[fim];
    int menor = inicio - 1;

    for (int j = inicio; j < fim; j++) {
        if (valores[j] <= pivo) {
            menor++;
            int guarda = valores[menor];
            valores[menor] = valores[j];
            valores[j] = guarda;
        }
    }

    int guarda = valores[menor + 1];
    valores[menor + 1] = valores[fim];
    valores[fim] = guarda;
    return menor + 1;
}

void ordenarRapido(int valores[], int inicio, int fim) {
    if (inicio >= fim) {
        return;
    }
    int corte = particionar(valores, inicio, fim);
    ordenarRapido(valores, inicio, corte - 1);
    ordenarRapido(valores, corte + 1, fim);
}
