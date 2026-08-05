int contar(int n) {
    if (n <= 1) {
        return 1;
    }
    switch (n % 2) {
        case 0:
            return contar(n - 1);
        case 1:
            return contar(n - 1) + contar(n - 2);
    }
    return 0;
}
