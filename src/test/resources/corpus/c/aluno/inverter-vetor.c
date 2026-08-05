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

    int esquerda = 0;
    int direita = quantidade - 1;
    while (esquerda < direita) {
        int guarda = numeros[esquerda];
        numeros[esquerda] = numeros[direita];
        numeros[direita] = guarda;
        esquerda++;
        direita--;
    }

    for (int i = 0; i < quantidade; i++) {
        printf("%d ", numeros[i]);
    }
    return 0;
}
