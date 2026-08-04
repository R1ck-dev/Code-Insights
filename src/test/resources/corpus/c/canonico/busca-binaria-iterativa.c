#include <stdio.h>

/* Busca binaria em vetor ordenado: a cada volta do laco o intervalo
   [inicio, fim] perde metade dos candidatos. */
int busca_binaria(const int vetor[], int tamanho, int alvo)
{
    int inicio = 0;
    int fim = tamanho - 1;
    int meio;

    while (inicio <= fim) {
        meio = (inicio + fim) / 2;

        if (vetor[meio] == alvo) {
            return meio;
        } else if (vetor[meio] < alvo) {
            inicio = meio + 1;   /* descarta a metade de baixo */
        } else {
            fim = meio - 1;      /* descarta a metade de cima */
        }
    }

    return -1;
}

int main(void)
{
    int ordenado[12] = {2, 5, 8, 12, 16, 23, 38, 41, 56, 72, 80, 91};
    int posicao = busca_binaria(ordenado, 12, 41);

    printf("Indice de 41: %d\n", posicao);

    return 0;
}
