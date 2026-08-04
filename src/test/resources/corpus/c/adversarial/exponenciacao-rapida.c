#include <stdio.h>

// Exponenciacao rapida (metodo binario): o expoente cai pela metade
// a cada volta e a base e elevada ao quadrado junto.
static long long potencia(long long base, int expo) {
    long long resultado = 1;

    while (expo > 0) {
        if (expo % 2 == 1) {
            resultado = resultado * base;
        }
        base = base * base;
        expo = expo / 2;
    }
    return resultado;
}

int main(void) {
    long long b;
    int e;

    printf("base e expoente: ");
    if (scanf("%lld %d", &b, &e) != 2 || e < 0) return 1;

    printf("%lld elevado a %d = %lld\n", b, e, potencia(b, e));
    return 0;
}
