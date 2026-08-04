#include <stdio.h>
#include <stdlib.h>

// funcao de comparacao exigida pelo qsort da stdlib
int compara(const void *a, const void *b) {
    int x = *(const int *) a;
    int y = *(const int *) b;
    if (x < y) return -1;
    if (x > y) return 1;
    return 0;
}

int main(void) {
    int n;
    if (scanf("%d", &n) != 1 || n <= 0) return 1;

    int *v = malloc(n * sizeof(int));
    if (v == NULL) return 1;

    for (int i = 0; i < n; i++) {
        if (scanf("%d", &v[i]) != 1) return 1;
    }

    // toda a ordenacao acontece dentro da biblioteca padrao
    qsort(v, n, sizeof(int), compara);

    for (int i = 0; i < n; i++) {
        printf("%d ", v[i]);
    }
    printf("\n");

    free(v);
    return 0;
}
