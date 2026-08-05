#include <stdio.h>

int main(void) {
    int contagem[10] = {0};
    int quantidade;

    if (scanf("%d", &quantidade) != 1) {
        return 1;
    }
    for (int i = 0; i < quantidade; i++) {
        int valor;
        scanf("%d", &valor);
        contagem[valor % 10]++;
    }

    for (int digito = 0; digito < 10; digito++) {
        printf("%d aparece %d vez(es)\n", digito, contagem[digito]);
    }
    return 0;
}
