#include <stdio.h>

void mover(int discos, char origem, char destino, char apoio) {
    if (discos == 0) {
        return;
    }
    mover(discos - 1, origem, apoio, destino);
    printf("disco %d: %c -> %c\n", discos, origem, destino);
    mover(discos - 1, apoio, destino, origem);
}

int main(void) {
    int n;

    if (scanf("%d", &n) != 1 || n < 0) {
        return 1;
    }

    mover(n, 'A', 'C', 'B');
    return 0;
}
