#include <stdio.h>
#include <string.h>

int main(void) {
    char s[1000];
    int vogais = 0;
    int maiusculas = 0;

    if (scanf("%999s", s) != 1) return 1;

    // strlen e reavaliado a cada volta e percorre a cadeia inteira de novo
    for (int i = 0; i < strlen(s); i++) {
        char c = s[i];
        if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') {
            vogais++;
        }
        if (c >= 'A' && c <= 'Z') {
            maiusculas++;
        }
    }

    printf("vogais: %d\n", vogais);
    printf("maiusculas: %d\n", maiusculas);
    return 0;
}
