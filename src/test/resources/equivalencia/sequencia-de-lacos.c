int somarDuasVezes(int valores[], int n) {
    int total = 0;
    for (int i = 0; i < n; i++) {
        total = total + valores[i];
    }
    for (int i = 0; i < n; i++) {
        total = total + valores[i] * 2;
    }
    return total;
}
