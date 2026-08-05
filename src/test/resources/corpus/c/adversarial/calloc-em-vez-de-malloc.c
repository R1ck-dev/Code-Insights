#include <stdio.h>
#include <stdlib.h>

int main(void) {
    int n;

    if (scanf("%d", &n) != 1 || n <= 0) {
        return 1;
    }

    int *zerados = calloc(n, sizeof(int));
    if (zerados == NULL) {
        return 1;
    }

    printf("primeiro %d ultimo %d\n", zerados[0], zerados[n - 1]);
    free(zerados);
    return 0;
}
