#include <stdio.h>
#include <string.h>

int main(void) {
    char frase[200];
    int i, j, tam, ehPalindromo;

    printf("Digite a frase: ");
    if (fgets(frase, sizeof(frase), stdin) == NULL) {
        return 0;
    }

    tam = (int) strlen(frase);

    // tira o \n que o fgets deixa no final
    if (tam > 0 && frase[tam - 1] == '\n') {
        frase[tam - 1] = '\0';
        tam = tam - 1;
    }

    ehPalindromo = 1;
    i = 0;
    j = tam - 1;
    while (i < j) {
        if (frase[i] != frase[j]) {
            ehPalindromo = 0;
        }
        i = i + 1;
        j = j - 1;
    }

    if (ehPalindromo == 1) {
        printf("SIM\n");
    } else {
        printf("NAO\n");
    }

    return 0;
}
