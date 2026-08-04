#include <stdio.h>

// Gera todas as permutacoes de n elementos por troca e desfaz-troca.
// Cada posicao k recebe, uma de cada vez, todos os elementos de k ate n-1.

static int n;
static long total;

static void permuta(int *v, int k) {
    int i, t;

    if (k == n) {          // v[0..n-1] e uma permutacao pronta
        total++;
        return;
    }

    for (i = k; i < n; i++) {
        t = v[k]; v[k] = v[i]; v[i] = t;   // troca
        permuta(v, k + 1);
        t = v[k]; v[k] = v[i]; v[i] = t;   // desfaz a troca
    }
}

int main(void) {
    int i;

    if (scanf("%d", &n) != 1 || n <= 0) return 1;

    int v[n];   // vetor de tamanho variavel (C99)
    for (i = 0; i < n; i++) v[i] = i + 1;

    permuta(v, 0);
    printf("permutacoes geradas: %ld\n", total);
    return 0;
}
