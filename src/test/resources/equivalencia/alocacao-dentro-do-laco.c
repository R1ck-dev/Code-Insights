#include <stdlib.h>

int processar(int n) {
    int total = 0;
    for (int i = 0; i < n; i++) {
        int *buffer = malloc(n * sizeof(int));
        if (buffer != NULL) {
            total = total + 1;
        }
        free(buffer);
    }
    return total;
}
