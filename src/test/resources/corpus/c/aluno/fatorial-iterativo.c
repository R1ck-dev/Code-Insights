#include <stdio.h>

int main(void) {
    int n;
    unsigned long long fatorial = 1;

    if (scanf("%d", &n) != 1 || n < 0) {
        return 1;
    }
    for (int i = 2; i <= n; i++) {
        fatorial = fatorial * i;
    }

    printf("%llu\n", fatorial);
    return 0;
}
