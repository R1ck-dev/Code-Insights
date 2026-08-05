#include <stdio.h>
#include <string.h>

int main(void) {
    char texto[200];
    int vogais = 0;

    printf("digite uma palavra: ");
    if (scanf("%199s", texto) != 1) {
        return 1;
    }

    int tamanho = strlen(texto);
    for (int i = 0; i < tamanho; i++) {
        char letra = texto[i];
        if (letra == 'a' || letra == 'e' || letra == 'i' || letra == 'o' || letra == 'u') {
            vogais++;
        }
    }

    printf("vogais: %d\n", vogais);
    return 0;
}
