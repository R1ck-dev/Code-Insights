#include <stdio.h>
#include <stdlib.h>

// Preenchimento por inundacao numa grade n x n guardada linearizada.
// A propria cor nova faz o papel do vetor de visitados: quem ja foi
// pintado nao casa mais com o alvo e a chamada retorna na hora.

static int n;
static char *grade;

static void preenche(int lin, int col, char alvo, char nova) {
    if (lin < 0 || lin >= n || col < 0 || col >= n) return;
    if (grade[lin * n + col] != alvo) return;

    grade[lin * n + col] = nova;
    preenche(lin + 1, col, alvo, nova);
    preenche(lin - 1, col, alvo, nova);
    preenche(lin, col + 1, alvo, nova);
    preenche(lin, col - 1, alvo, nova);
}

int main(void) {
    int i, j;

    if (scanf("%d", &n) != 1 || n <= 0) return 1;

    grade = (char *) malloc((size_t) n * (size_t) n);
    if (grade == NULL) return 1;

    for (i = 0; i < n; i++)
        for (j = 0; j < n; j++)
            scanf(" %c", &grade[i * n + j]);

    preenche(0, 0, grade[0], '*');

    for (i = 0; i < n; i++) {
        for (j = 0; j < n; j++) putchar(grade[i * n + j]);
        putchar('\n');
    }

    free(grade);
    return 0;
}
