int contar(int n) {
    int i = 0;
    int passos = 0;
    do {
        passos++;
        i++;
    } while (i < n);
    return passos;
}
