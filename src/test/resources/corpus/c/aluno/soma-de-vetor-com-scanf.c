#include <stdio.h>

#define MAX 100

int main(void) {
    int v[MAX];
    int n, i;
    long soma = 0;

    printf("Digite a quantidade de numeros: ");
    scanf("%d", &n);

    if (n < 0 || n > MAX) {
        printf("Quantidade invalida\n");
        return 1;
    }

    // le os n numeros informados pelo usuario
    for (i = 0; i < n; i++) {
        printf("Numero %d: ", i + 1);
        scanf("%d", &v[i]);
    }

    // soma tudo o que foi lido
    for (i = 0; i < n; i++) {
        soma += v[i];
    }

    printf("A soma dos %d numeros e %ld\n", n, soma);
    return 0;
}
