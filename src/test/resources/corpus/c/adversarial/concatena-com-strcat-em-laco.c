#include <stdio.h>
#include <string.h>

int main(void) {
    int n;
    char destino[4096];
    char pedaco[64];

    if (scanf("%d", &n) != 1 || n <= 0) return 1;

    destino[0] = '\0';

    for (int i = 0; i < n; i++) {
        if (scanf("%63s", pedaco) != 1) break;
        // strcat anda ate o fim de destino antes de copiar:
        // o trecho ja montado e percorrido de novo a cada volta
        strcat(destino, pedaco);
        strcat(destino, " ");
    }

    printf("%s\n", destino);
    printf("tamanho final: %d\n", (int) strlen(destino));
    return 0;
}
