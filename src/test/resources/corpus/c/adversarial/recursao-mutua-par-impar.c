#include <stdio.h>

int ehImpar(int n);

int ehPar(int n) {
    if (n == 0) {
        return 1;
    }
    return ehImpar(n - 1);
}

int ehImpar(int n) {
    if (n == 0) {
        return 0;
    }
    return ehPar(n - 1);
}

int main(void) {
    int n;

    if (scanf("%d", &n) != 1 || n < 0) {
        return 1;
    }

    printf("%d\n", ehPar(n));
    return 0;
}
