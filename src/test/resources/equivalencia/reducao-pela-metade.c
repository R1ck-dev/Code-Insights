int buscar(int ordenado[], int inicio, int fim, int alvo) {
    if (inicio > fim) {
        return -1;
    }
    int meio = (inicio + fim) / 2;
    if (ordenado[meio] == alvo) {
        return meio;
    }
    if (ordenado[meio] < alvo) {
        return buscar(ordenado, meio + 1, fim, alvo);
    }
    return buscar(ordenado, inicio, meio - 1, alvo);
}
