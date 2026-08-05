#include <stdio.h>

int main(void) {
    int n;

    if (scanf("%d", &n) != 1 || n <= 0) {
        return 1;
    }

    int valores[n];
    for (int i = 0; i < n; i++) {
        valores[i] = i * i;
    }

    long soma = 0;
    for (int i = 0; i < n; i++) {
        soma = soma + valores[i];
    }

    printf("%ld\n", soma);
    return 0;
}
