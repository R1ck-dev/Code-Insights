#include <stdio.h>

int main(void) {
    int n, i, ehPrimo;

    printf("Digite um numero: ");
    scanf("%d", &n);

    ehPrimo = 1;
    if (n < 2) {
        ehPrimo = 0;
    }

    // testa todos os divisores de 2 ate n-1
    for (i = 2; i < n; i++) {
        if (n % i == 0) {
            ehPrimo = 0;
        }
    }

    if (ehPrimo == 1) {
        printf("PRIMO\n");
    } else {
        printf("NAO PRIMO\n");
    }

    return 0;
}
