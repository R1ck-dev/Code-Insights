// Solucoes em C. Nem todo desafio tem versao em C: os que dependem de estruturas da biblioteca
// padrao do Java (mapa, fila) ficariam artificiais aqui, e um caso artificial mede mal.

export const C = {
    'two-sum': {
        ingenua: `#include <stdio.h>

int encontrar_par(const int numeros[], int tamanho, int alvo, int par[2]) {
    for (int i = 0; i < tamanho; i++) {
        for (int j = i + 1; j < tamanho; j++) {
            if (numeros[i] + numeros[j] == alvo) {
                par[0] = i;
                par[1] = j;
                return 1;
            }
        }
    }
    return 0;
}`,
        refinada: `#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define BALDES 4096

typedef struct No {
    int valor;
    int indice;
    struct No *proximo;
} No;

static int balde_de(int valor) {
    int posicao = valor % BALDES;
    if (posicao < 0) {
        posicao = posicao + BALDES;
    }
    return posicao;
}

int encontrar_par(const int numeros[], int tamanho, int alvo, int par[2]) {
    No *tabela[BALDES];
    memset(tabela, 0, sizeof(tabela));

    for (int i = 0; i < tamanho; i++) {
        int procurado = alvo - numeros[i];
        for (No *no = tabela[balde_de(procurado)]; no != NULL; no = no->proximo) {
            if (no->valor == procurado) {
                par[0] = no->indice;
                par[1] = i;
                return 1;
            }
        }
        No *novo = malloc(sizeof(No));
        novo->valor = numeros[i];
        novo->indice = i;
        novo->proximo = tabela[balde_de(numeros[i])];
        tabela[balde_de(numeros[i])] = novo;
    }
    return 0;
}`,
    },

    'busca-elemento': {
        ingenua: `#include <stdio.h>

int procurar(const int vetor[], int tamanho, int alvo) {
    for (int i = 0; i < tamanho; i++) {
        if (vetor[i] == alvo) {
            return i;
        }
    }
    return -1;
}`,
        refinada: `#include <stdio.h>

int procurar(const int vetor[], int tamanho, int alvo) {
    int inicio = 0;
    int fim = tamanho - 1;
    while (inicio <= fim) {
        int meio = inicio + (fim - inicio) / 2;
        if (vetor[meio] == alvo) {
            return meio;
        }
        if (vetor[meio] < alvo) {
            inicio = meio + 1;
        } else {
            fim = meio - 1;
        }
    }
    return -1;
}`,
    },

    fibonacci: {
        ingenua: `#include <stdio.h>

long long termo(int n) {
    if (n <= 1) {
        return n;
    }
    return termo(n - 1) + termo(n - 2);
}`,
        refinada: `#include <stdio.h>

long long termo(int n) {
    if (n <= 1) {
        return n;
    }
    long long anterior = 0;
    long long atual = 1;
    for (int i = 2; i <= n; i++) {
        long long proximo = anterior + atual;
        anterior = atual;
        atual = proximo;
    }
    return atual;
}`,
    },

    ordenacao: {
        ingenua: `#include <stdio.h>

void ordenar(int vetor[], int tamanho) {
    for (int i = 0; i < tamanho - 1; i++) {
        for (int j = 0; j < tamanho - 1 - i; j++) {
            if (vetor[j] > vetor[j + 1]) {
                int troca = vetor[j];
                vetor[j] = vetor[j + 1];
                vetor[j + 1] = troca;
            }
        }
    }
}`,
        refinada: `#include <stdio.h>
#include <stdlib.h>

static void intercalar(int vetor[], int inicio, int meio, int fim) {
    int tamanho = fim - inicio + 1;
    int *apoio = malloc(sizeof(int) * tamanho);
    int i = inicio;
    int j = meio + 1;
    int k = 0;

    while (i <= meio && j <= fim) {
        if (vetor[i] <= vetor[j]) {
            apoio[k] = vetor[i];
            i++;
        } else {
            apoio[k] = vetor[j];
            j++;
        }
        k++;
    }
    while (i <= meio) {
        apoio[k] = vetor[i];
        i++;
        k++;
    }
    while (j <= fim) {
        apoio[k] = vetor[j];
        j++;
        k++;
    }
    for (int p = 0; p < tamanho; p++) {
        vetor[inicio + p] = apoio[p];
    }
    free(apoio);
}

void ordenar(int vetor[], int inicio, int fim) {
    if (inicio >= fim) {
        return;
    }
    int meio = inicio + (fim - inicio) / 2;
    ordenar(vetor, inicio, meio);
    ordenar(vetor, meio + 1, fim);
    intercalar(vetor, inicio, meio, fim);
}`,
    },

    palindromo: {
        ingenua: `#include <stdio.h>
#include <string.h>
#include <ctype.h>

int eh_palindromo(const char *texto) {
    char limpo[1024];
    int tamanho = 0;
    for (int i = 0; texto[i] != '\\0'; i++) {
        if (texto[i] != ' ') {
            limpo[tamanho] = tolower(texto[i]);
            tamanho++;
        }
    }
    limpo[tamanho] = '\\0';

    char invertido[1024];
    for (int i = 0; i < tamanho; i++) {
        invertido[i] = limpo[tamanho - 1 - i];
    }
    invertido[tamanho] = '\\0';

    return strcmp(limpo, invertido) == 0;
}`,
        refinada: `#include <stdio.h>
#include <string.h>
#include <ctype.h>

int eh_palindromo(const char *texto) {
    int i = 0;
    int j = strlen(texto) - 1;
    while (i < j) {
        if (texto[i] == ' ') {
            i++;
        } else if (texto[j] == ' ') {
            j--;
        } else if (tolower(texto[i]) != tolower(texto[j])) {
            return 0;
        } else {
            i++;
            j--;
        }
    }
    return 1;
}`,
    },

    primos: {
        ingenua: `#include <stdio.h>

int contar(int limite) {
    int total = 0;
    for (int n = 2; n <= limite; n++) {
        int primo = 1;
        for (int d = 2; d < n; d++) {
            if (n % d == 0) {
                primo = 0;
                break;
            }
        }
        if (primo) {
            total++;
        }
    }
    return total;
}`,
        refinada: `#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int contar(int limite) {
    char *composto = calloc(limite + 1, sizeof(char));
    int total = 0;
    for (int n = 2; n <= limite; n++) {
        if (composto[n] == 0) {
            total++;
            for (int m = n + n; m <= limite; m += n) {
                composto[m] = 1;
            }
        }
    }
    free(composto);
    return total;
}`,
    },

    anagrama: {
        ingenua: `#include <stdio.h>
#include <string.h>

int sao_anagramas(const char *a, const char *b) {
    int tamanho = strlen(a);
    if (tamanho != (int) strlen(b)) {
        return 0;
    }
    char usado[1024];
    memset(usado, 0, sizeof(usado));

    for (int i = 0; i < tamanho; i++) {
        int achou = 0;
        for (int j = 0; j < tamanho; j++) {
            if (usado[j] == 0 && a[i] == b[j]) {
                usado[j] = 1;
                achou = 1;
                break;
            }
        }
        if (achou == 0) {
            return 0;
        }
    }
    return 1;
}`,
        refinada: `#include <stdio.h>
#include <string.h>

int sao_anagramas(const char *a, const char *b) {
    int tamanho = strlen(a);
    if (tamanho != (int) strlen(b)) {
        return 0;
    }
    int frequencia[256];
    memset(frequencia, 0, sizeof(frequencia));

    for (int i = 0; i < tamanho; i++) {
        frequencia[(unsigned char) a[i]]++;
        frequencia[(unsigned char) b[i]]--;
    }
    for (int i = 0; i < 256; i++) {
        if (frequencia[i] != 0) {
            return 0;
        }
    }
    return 1;
}`,
    },
};
