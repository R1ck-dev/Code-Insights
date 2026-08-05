#include <stdio.h>

int main(void) {
    int matriz[50][50];
    int n;

    if (scanf("%d", &n) != 1 || n > 50) {
        return 1;
    }
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            scanf("%d", &matriz[i][j]);
        }
    }

    long soma = 0;
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            soma = soma + matriz[i][j];
        }
    }

    printf("%ld\n", soma);
    return 0;
}
