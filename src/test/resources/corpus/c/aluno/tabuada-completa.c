#include <stdio.h>

int main(void) {
    int i, j;

    printf("Tabuada de 1 a 10\n\n");

    // laco de fora escolhe qual tabuada sera impressa
    for (i = 1; i <= 10; i++) {
        printf("Tabuada do %d\n", i);

        // laco de dentro percorre os multiplicadores
        for (j = 1; j <= 10; j++) {
            printf("%d x %d = %d\n", i, j, i * j);
        }

        printf("\n");
    }

    return 0;
}
