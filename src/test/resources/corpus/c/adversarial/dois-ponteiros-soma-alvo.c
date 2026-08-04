#include <stdio.h>
#include <stdlib.h>

// procura um par que some o alvo num vetor ja ordenado
int existe_par_com_soma(const int *ordenado, int n, int alvo) {
    int esquerda = 0;
    int direita = n - 1;
    while (esquerda < direita) {
        int soma = ordenado[esquerda] + ordenado[direita];
        if (soma == alvo) return 1;
        if (soma < alvo) esquerda++;
        else direita--;
    }
    return 0;
}

int main(void) {
    int n, alvo;
    if (scanf("%d %d", &n, &alvo) != 2 || n <= 0) return 1;

    int *v = malloc(n * sizeof(int));
    if (v == NULL) return 1;

    for (int i = 0; i < n; i++) {
        if (scanf("%d", &v[i]) != 1) return 1;
    }

    if (existe_par_com_soma(v, n, alvo)) {
        printf("existe par com soma %d\n", alvo);
    } else {
        printf("nao existe par com soma %d\n", alvo);
    }

    free(v);
    return 0;
}
