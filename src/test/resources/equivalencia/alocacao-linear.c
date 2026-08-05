#include <stdlib.h>

int *copiar(int origem[], int n) {
    int *destino = malloc(n * sizeof(int));
    for (int i = 0; i < n; i++) {
        destino[i] = origem[i];
    }
    return destino;
}
