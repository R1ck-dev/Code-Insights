#include <stdio.h>

int main(void) {
    unsigned int n;

    if (scanf("%u", &n) != 1) {
        return 1;
    }

    int bits = 0;
    for (unsigned int i = n; i > 0; i >>= 1) {
        if (i & 1u) {
            bits++;
        }
    }

    printf("%d\n", bits);
    return 0;
}
