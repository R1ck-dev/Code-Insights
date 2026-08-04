#include <stdio.h>
#include <stdlib.h>

/* Intercala as duas metades ja ordenadas num vetor temporario do tamanho do trecho. */
static void intercala(int v[], int inicio, int meio, int fim) {
    int *temporario = malloc((size_t) (fim - inicio + 1) * sizeof(int));
    int i = inicio, j = meio + 1, k = 0, t;

    if (temporario == NULL) return;
    while (i <= meio && j <= fim) {
        if (v[i] <= v[j]) temporario[k++] = v[i++];
        else temporario[k++] = v[j++];
    }
    while (i <= meio) temporario[k++] = v[i++];
    while (j <= fim) temporario[k++] = v[j++];
    for (t = 0; t < k; t++) v[inicio + t] = temporario[t];
    free(temporario);
}

/* Merge sort: divide o trecho ao meio, ordena as duas metades e intercala. */
static void ordena(int v[], int inicio, int fim) {
    int meio;

    if (inicio >= fim) return;
    meio = inicio + (fim - inicio) / 2;
    ordena(v, inicio, meio);
    ordena(v, meio + 1, fim);
    intercala(v, inicio, meio, fim);
}

int main(void) {
    int n, i;
    int *v;

    if (scanf("%d", &n) != 1 || n <= 0) return 1;
    v = malloc((size_t) n * sizeof(int));
    if (v == NULL) return 1;
    for (i = 0; i < n; i++) scanf("%d", &v[i]);

    ordena(v, 0, n - 1);
    for (i = 0; i < n; i++) printf("%d ", v[i]);
    printf("\n");
    free(v);
    return 0;
}
