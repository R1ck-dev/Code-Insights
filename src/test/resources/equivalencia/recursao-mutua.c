int ehImpar(int n);

int ehPar(int n) {
    if (n == 0) {
        return 1;
    }
    return ehImpar(n - 1);
}

int ehImpar(int n) {
    if (n == 0) {
        return 0;
    }
    return ehPar(n - 1);
}
