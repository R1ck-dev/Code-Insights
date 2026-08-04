#include <stdio.h>

/* Fibonacci ingenuo: as duas auto-chamadas rodam sempre, uma depois da outra, e
   nada e guardado, entao o mesmo termo volta a ser recalculado do zero. */
long long fibonacci(int n) {
    if (n < 2) {
        return n;
    }
    return fibonacci(n - 1) + fibonacci(n - 2);
}

int main(void) {
    int n;

    printf("Digite n: ");
    if (scanf("%d", &n) != 1 || n < 0) {
        printf("Entrada invalida.\n");
        return 1;
    }

    printf("fib(%d) = %lld\n", n, fibonacci(n));
    return 0;
}
