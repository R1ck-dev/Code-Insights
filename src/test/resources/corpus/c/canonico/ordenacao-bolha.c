#include <stdio.h>

/* Bolha classico: a cada passada o maior elemento ainda fora do lugar
   sobe ate o fim do trecho desordenado, trocando com o vizinho. */
void ordenacao_bolha(int vetor[], int tamanho)
{
    int i, j, temporario;

    for (i = 0; i < tamanho - 1; i++) {
        for (j = 0; j < tamanho - 1 - i; j++) {
            if (vetor[j] > vetor[j + 1]) {
                temporario = vetor[j];
                vetor[j] = vetor[j + 1];
                vetor[j + 1] = temporario;
            }
        }
    }
}

int main(void)
{
    int dados[8] = {45, 12, 78, 3, 61, 29, 7, 90};
    int i;

    ordenacao_bolha(dados, 8);

    for (i = 0; i < 8; i++) {
        printf("%d ", dados[i]);
    }
    printf("\n");

    return 0;
}
