#include <stdio.h>

int main(void) {
    double valores[100];
    int quantidade;

    if (scanf("%d", &quantidade) != 1 || quantidade <= 0 || quantidade > 100) {
        return 1;
    }
    for (int i = 0; i < quantidade; i++) {
        scanf("%lf", &valores[i]);
    }

    double soma = 0.0;
    for (int i = 0; i < quantidade; i++) {
        soma = soma + valores[i];
    }
    double media = soma / quantidade;

    double acumulado = 0.0;
    for (int i = 0; i < quantidade; i++) {
        double diferenca = valores[i] - media;
        acumulado = acumulado + diferenca * diferenca;
    }

    printf("media %.2f variancia %.2f\n", media, acumulado / quantidade);
    return 0;
}
