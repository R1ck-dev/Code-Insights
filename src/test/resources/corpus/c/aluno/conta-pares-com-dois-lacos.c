#include <stdio.h>

#define MAX 200

int main(void) {
    int v[MAX];
    int n, i, j;
    int pares = 0;

    printf("Quantidade de elementos: ");
    scanf("%d", &n);

    if (n <= 0 || n > MAX) {
        printf("Quantidade invalida\n");
        return 1;
    }

    for (i = 0; i < n; i++) {
        printf("Elemento %d: ", i + 1);
        scanf("%d", &v[i]);
    }

    // testa cada par (i, j) com j sempre a frente de i
    for (i = 0; i < n; i++) {
        for (j = i + 1; j < n; j++) {
            if (v[i] == v[j]) {
                pares++;
            }
        }
    }

    printf("Foram encontrados %d pares de valores iguais\n", pares);
    return 0;
}
