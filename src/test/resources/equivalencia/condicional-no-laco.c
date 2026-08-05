int classificar(int valores[], int n) {
    int saldo = 0;
    for (int i = 0; i < n; i++) {
        if (valores[i] % 2 == 0) {
            saldo++;
        } else {
            saldo--;
        }
    }
    return saldo;
}
