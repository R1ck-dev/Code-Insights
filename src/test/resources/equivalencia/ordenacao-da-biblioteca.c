#include <stdlib.h>

static int comparar(const void *a, const void *b) {
    return (*(const int *)a) - (*(const int *)b);
}

void ordenar(int valores[], int n) {
    qsort(valores, n, sizeof(int), comparar);
}
