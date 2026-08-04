#include <stdio.h>

/* Percorre o vetor do inicio ao fim e devolve o indice da primeira
   ocorrencia do alvo, ou -1 quando o valor nao esta presente. */
int busca_linear(const int vetor[], int tamanho, int alvo)
{
    int i;

    for (i = 0; i < tamanho; i++) {
        if (vetor[i] == alvo) {
            return i;
        }
    }

    return -1;
}

int main(void)
{
    int dados[10] = {14, 3, 27, 8, 91, 5, 62, 30, 47, 19};
    int posicao = busca_linear(dados, 10, 62);

    if (posicao >= 0) {
        printf("Valor encontrado no indice %d\n", posicao);
    } else {
        printf("Valor ausente no vetor\n");
    }

    return 0;
}
