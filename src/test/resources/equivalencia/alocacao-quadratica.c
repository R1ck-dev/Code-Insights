#include <stdlib.h>

int *criarGrade(int n) {
    int *grade = malloc(n * n * sizeof(int));
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            grade[i * n + j] = 0;
        }
    }
    return grade;
}
