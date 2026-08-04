#include <stdio.h>

#define TAM 50

int main(void) {
    int v[TAM];
    int n, i, maior;

    printf("Quantos numeros voce vai digitar? ");
    scanf("%d", &n);

    if (n <= 0 || n > TAM) {
        printf("Quantidade fora do permitido\n");
        return 1;
    }

    for (i = 0; i < n; i++) {
        printf("Digite o numero %d: ", i + 1);
        scanf("%d", &v[i]);
    }

    maior = v[0];

    // compara cada elemento com o maior encontrado ate agora
    for (i = 1; i < n; i++) {
        if (v[i] > maior) {
            maior = v[i];
        }
    }

    printf("O maior numero digitado foi %d\n", maior);
    return 0;
}
