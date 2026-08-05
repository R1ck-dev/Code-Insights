#include <stdio.h>

int main(void) {
    int numeros[100];
    int quantidade;

    if (scanf("%d", &quantidade) != 1 || quantidade > 100) {
        return 1;
    }
    for (int i = 0; i < quantidade; i++) {
        scanf("%d", &numeros[i]);
    }

    for (int i = 0; i < quantidade - 1; i++) {
        int trocou = 0;
        for (int j = 0; j < quantidade - 1 - i; j++) {
            if (numeros[j] > numeros[j + 1]) {
                int guarda = numeros[j];
                numeros[j] = numeros[j + 1];
                numeros[j + 1] = guarda;
                trocou = 1;
            }
        }
        if (trocou == 0) {
            break;
        }
    }

    for (int i = 0; i < quantidade; i++) {
        printf("%d ", numeros[i]);
    }
    return 0;
}
