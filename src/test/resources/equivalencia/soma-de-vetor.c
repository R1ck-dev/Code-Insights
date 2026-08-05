int somar(int valores[], int n) {
    int total = 0;
    for (int i = 0; i < n; i++) {
        total = total + valores[i];
    }
    return total;
}
