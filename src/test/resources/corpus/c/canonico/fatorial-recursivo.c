#include <stdio.h>

/* Fatorial classico por recursao: cada chamada resolve n a partir de n - 1. */
unsigned long long fatorial(int n) {
    if (n <= 1) {
        return 1ULL;
    }
    return (unsigned long long) n * fatorial(n - 1);
}

int main(void) {
    int n;

    printf("Digite n: ");
    if (scanf("%d", &n) != 1 || n < 0) {
        printf("Entrada invalida.\n");
        return 1;
    }

    printf("%d! = %llu\n", n, fatorial(n));
    return 0;
}
