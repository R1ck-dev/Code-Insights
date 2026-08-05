#include <stdio.h>

int main(void) {
    int matriz[50][50];
    int n;
    int alvo;
    int achouLinha = -1;
    int achouColuna = -1;

    if (scanf("%d %d", &n, &alvo) != 2 || n > 50) {
        return 1;
    }
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            scanf("%d", &matriz[i][j]);
        }
    }

    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            if (matriz[i][j] == alvo) {
                achouLinha = i;
                achouColuna = j;
                goto encontrado;
            }
        }
    }

encontrado:
    printf("%d %d\n", achouLinha, achouColuna);
    return 0;
}
