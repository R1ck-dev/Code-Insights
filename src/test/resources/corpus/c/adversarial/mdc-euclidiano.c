#include <stdio.h>

// Algoritmo de Euclides: a cada volta o par (a, b) vira (b, a % b),
// e o resto encolhe o argumento depressa.
static int mdc(int a, int b) {
    int r;

    while (b != 0) {
        r = a % b;
        a = b;
        b = r;
    }
    return a;
}

int main(void) {
    int x, y;

    printf("dois inteiros: ");
    if (scanf("%d %d", &x, &y) != 2) return 1;

    if (x < 0) x = -x;
    if (y < 0) y = -y;

    printf("mdc(%d, %d) = %d\n", x, y, mdc(x, y));
    return 0;
}
