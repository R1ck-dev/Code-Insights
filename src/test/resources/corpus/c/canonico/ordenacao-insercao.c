#include <stdio.h>

/* Insercao: mantem o prefixo do vetor sempre ordenado e desloca para a
   direita todos os elementos maiores que a chave, ate abrir o lugar dela. */
void ordenacao_insercao(int vetor[], int tamanho)
{
    int i, j, chave;

    for (i = 1; i < tamanho; i++) {
        chave = vetor[i];
        j = i - 1;

        while (j >= 0 && vetor[j] > chave) {
            vetor[j + 1] = vetor[j];
            j--;
        }

        vetor[j + 1] = chave;
    }
}

int main(void)
{
    int dados[9] = {31, 4, 57, 22, 8, 96, 13, 70, 45};
    int i;

    ordenacao_insercao(dados, 9);

    for (i = 0; i < 9; i++) {
        printf("%d ", dados[i]);
    }
    printf("\n");

    return 0;
}
