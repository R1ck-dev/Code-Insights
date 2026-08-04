#include <stdio.h>

/* Busca binaria recursiva. Os dois ramos do if sao exclusivos: por nivel roda
   apenas uma das duas auto-chamadas, e ela recebe metade do intervalo. */
int busca(const int v[], int alvo, int inicio, int fim) {
    int meio;

    if (inicio > fim) {
        return -1;
    }
    meio = inicio + (fim - inicio) / 2;

    if (v[meio] == alvo) {
        return meio;
    }
    if (v[meio] < alvo) {
        return busca(v, alvo, meio + 1, fim);
    }
    return busca(v, alvo, inicio, meio - 1);
}

int main(void) {
    /* A tabela ja vem ordenada dentro do programa; a entrada diz quantas
       posicoes dela estao em uso e qual valor procurar. */
    static const int tabela[] = {2, 5, 8, 13, 21, 34, 55, 89, 144, 233};
    int n;
    int alvo;

    if (scanf("%d %d", &n, &alvo) != 2) {
        return 1;
    }
    printf("posicao: %d\n", busca(tabela, alvo, 0, n - 1));
    return 0;
}
