#include <stdio.h>

int main(void) {
    int n;

    if (scanf("%d", &n) != 1 || n < 0) {
        return 1;
    }

    unsigned long long anterior = 0;
    unsigned long long atual = 1;
    for (int i = 2; i <= n; i++) {
        unsigned long long proximo = anterior + atual;
        anterior = atual;
        atual = proximo;
    }

    printf("%llu\n", n == 0 ? 0 : atual);
    return 0;
}
