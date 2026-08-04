#include <stdio.h>

int main(void) {
    int n, linha, coluna;

    printf("Altura da piramide: ");
    scanf("%d", &n);

    // na linha 1 sai 1 asterisco, na linha 2 saem 2, e assim por diante
    for (linha = 1; linha <= n; linha++) {
        for (coluna = 1; coluna <= linha; coluna++) {
            printf("*");
        }
        printf("\n");
    }

    printf("Fim\n");

    return 0;
}
