// Solucoes em Java. Ver o cabecalho de desafios.mjs para o porque do par ingenua/refinada.

export const JAVA = {
    'two-sum': {
        ingenua: `public class TwoSum {

    public static int[] encontrarPar(int[] numeros, int alvo) {
        for (int i = 0; i < numeros.length; i++) {
            for (int j = i + 1; j < numeros.length; j++) {
                if (numeros[i] + numeros[j] == alvo) {
                    return new int[] { i, j };
                }
            }
        }
        return new int[] { -1, -1 };
    }
}`,
        refinada: `import java.util.HashMap;
import java.util.Map;

public class TwoSum {

    public static int[] encontrarPar(int[] numeros, int alvo) {
        Map<Integer, Integer> vistos = new HashMap<>();
        for (int i = 0; i < numeros.length; i++) {
            Integer anterior = vistos.get(alvo - numeros[i]);
            if (anterior != null) {
                return new int[] { anterior, i };
            }
            vistos.put(numeros[i], i);
        }
        return new int[] { -1, -1 };
    }
}`,
    },

    'maior-soma': {
        ingenua: `public class MaiorSoma {

    public static int maiorSoma(int[] valores) {
        int melhor = valores[0];
        for (int i = 0; i < valores.length; i++) {
            int soma = 0;
            for (int j = i; j < valores.length; j++) {
                soma += valores[j];
                if (soma > melhor) {
                    melhor = soma;
                }
            }
        }
        return melhor;
    }
}`,
        refinada: `public class MaiorSoma {

    public static int maiorSoma(int[] valores) {
        int melhor = valores[0];
        int atual = valores[0];
        for (int i = 1; i < valores.length; i++) {
            if (atual < 0) {
                atual = valores[i];
            } else {
                atual = atual + valores[i];
            }
            if (atual > melhor) {
                melhor = atual;
            }
        }
        return melhor;
    }
}`,
    },

    'busca-elemento': {
        ingenua: `public class Busca {

    public static int procurar(int[] vetor, int alvo) {
        for (int i = 0; i < vetor.length; i++) {
            if (vetor[i] == alvo) {
                return i;
            }
        }
        return -1;
    }
}`,
        refinada: `public class Busca {

    public static int procurar(int[] vetor, int alvo) {
        int inicio = 0;
        int fim = vetor.length - 1;
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
    }
}`,
    },

    fibonacci: {
        ingenua: `public class Fibonacci {

    public static long termo(int n) {
        if (n <= 1) {
            return n;
        }
        return termo(n - 1) + termo(n - 2);
    }
}`,
        refinada: `public class Fibonacci {

    public static long termo(int n) {
        if (n <= 1) {
            return n;
        }
        long anterior = 0;
        long atual = 1;
        for (int i = 2; i <= n; i++) {
            long proximo = anterior + atual;
            anterior = atual;
            atual = proximo;
        }
        return atual;
    }
}`,
    },

    ordenacao: {
        ingenua: `public class Ordenacao {

    public static void ordenar(int[] vetor) {
        for (int i = 0; i < vetor.length - 1; i++) {
            for (int j = 0; j < vetor.length - 1 - i; j++) {
                if (vetor[j] > vetor[j + 1]) {
                    int troca = vetor[j];
                    vetor[j] = vetor[j + 1];
                    vetor[j + 1] = troca;
                }
            }
        }
    }
}`,
        refinada: `public class Ordenacao {

    public static void ordenar(int[] vetor, int inicio, int fim) {
        if (inicio >= fim) {
            return;
        }
        int meio = inicio + (fim - inicio) / 2;
        ordenar(vetor, inicio, meio);
        ordenar(vetor, meio + 1, fim);
        intercalar(vetor, inicio, meio, fim);
    }

    private static void intercalar(int[] vetor, int inicio, int meio, int fim) {
        int[] apoio = new int[fim - inicio + 1];
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
        for (int p = 0; p < apoio.length; p++) {
            vetor[inicio + p] = apoio[p];
        }
    }
}`,
    },

    palindromo: {
        ingenua: `public class Palindromo {

    public static boolean ehPalindromo(String texto) {
        String limpo = "";
        for (int i = 0; i < texto.length(); i++) {
            char atual = texto.charAt(i);
            if (atual != ' ') {
                limpo = limpo + Character.toLowerCase(atual);
            }
        }
        String invertido = "";
        for (int i = limpo.length() - 1; i >= 0; i--) {
            invertido = invertido + limpo.charAt(i);
        }
        return limpo.equals(invertido);
    }
}`,
        refinada: `public class Palindromo {

    public static boolean ehPalindromo(String texto) {
        int i = 0;
        int j = texto.length() - 1;
        while (i < j) {
            if (texto.charAt(i) == ' ') {
                i++;
            } else if (texto.charAt(j) == ' ') {
                j--;
            } else if (Character.toLowerCase(texto.charAt(i)) != Character.toLowerCase(texto.charAt(j))) {
                return false;
            } else {
                i++;
                j--;
            }
        }
        return true;
    }
}`,
    },

    primos: {
        ingenua: `public class Primos {

    public static int contar(int limite) {
        int total = 0;
        for (int n = 2; n <= limite; n++) {
            boolean primo = true;
            for (int d = 2; d < n; d++) {
                if (n % d == 0) {
                    primo = false;
                    break;
                }
            }
            if (primo) {
                total++;
            }
        }
        return total;
    }
}`,
        refinada: `public class Primos {

    public static int contar(int limite) {
        boolean[] composto = new boolean[limite + 1];
        int total = 0;
        for (int n = 2; n <= limite; n++) {
            if (composto[n] == false) {
                total++;
                for (int m = n + n; m <= limite; m += n) {
                    composto[m] = true;
                }
            }
        }
        return total;
    }
}`,
    },

    anagrama: {
        ingenua: `public class Anagrama {

    public static boolean saoAnagramas(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        boolean[] usado = new boolean[b.length()];
        for (int i = 0; i < a.length(); i++) {
            boolean achou = false;
            for (int j = 0; j < b.length(); j++) {
                if (usado[j] == false && a.charAt(i) == b.charAt(j)) {
                    usado[j] = true;
                    achou = true;
                    break;
                }
            }
            if (achou == false) {
                return false;
            }
        }
        return true;
    }
}`,
        refinada: `public class Anagrama {

    public static boolean saoAnagramas(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int[] frequencia = new int[256];
        for (int i = 0; i < a.length(); i++) {
            frequencia[a.charAt(i)]++;
            frequencia[b.charAt(i)]--;
        }
        for (int i = 0; i < frequencia.length; i++) {
            if (frequencia[i] != 0) {
                return false;
            }
        }
        return true;
    }
}`,
    },

    ilhas: {
        ingenua: `public class Ilhas {

    public static int contar(char[][] grade) {
        boolean[][] visitado = new boolean[grade.length][grade[0].length];
        int total = 0;
        for (int i = 0; i < grade.length; i++) {
            for (int j = 0; j < grade[0].length; j++) {
                if (grade[i][j] == '1' && visitado[i][j] == false) {
                    total++;
                    marcar(grade, visitado, i, j);
                }
            }
        }
        return total;
    }

    private static void marcar(char[][] grade, boolean[][] visitado, int i, int j) {
        if (i < 0 || j < 0 || i >= grade.length || j >= grade[0].length) {
            return;
        }
        if (visitado[i][j] || grade[i][j] == '0') {
            return;
        }
        visitado[i][j] = true;
        marcar(grade, visitado, i + 1, j);
        marcar(grade, visitado, i - 1, j);
        marcar(grade, visitado, i, j + 1);
        marcar(grade, visitado, i, j - 1);
    }
}`,
        refinada: `import java.util.ArrayDeque;
import java.util.Deque;

public class Ilhas {

    public static int contar(char[][] grade) {
        int total = 0;
        for (int i = 0; i < grade.length; i++) {
            for (int j = 0; j < grade[0].length; j++) {
                if (grade[i][j] == '1') {
                    total++;
                    afundar(grade, i, j);
                }
            }
        }
        return total;
    }

    private static void afundar(char[][] grade, int origemI, int origemJ) {
        Deque<int[]> fila = new ArrayDeque<>();
        fila.add(new int[] { origemI, origemJ });
        grade[origemI][origemJ] = '0';
        int[] desviosI = { 1, -1, 0, 0 };
        int[] desviosJ = { 0, 0, 1, -1 };
        while (fila.isEmpty() == false) {
            int[] atual = fila.poll();
            for (int d = 0; d < 4; d++) {
                int i = atual[0] + desviosI[d];
                int j = atual[1] + desviosJ[d];
                if (i >= 0 && j >= 0 && i < grade.length && j < grade[0].length && grade[i][j] == '1') {
                    grade[i][j] = '0';
                    fila.add(new int[] { i, j });
                }
            }
        }
    }
}`,
    },

    mochila: {
        ingenua: `public class Mochila {

    public static int melhorValor(int[] pesos, int[] valores, int capacidade, int i) {
        if (i >= pesos.length || capacidade <= 0) {
            return 0;
        }
        int semLevar = melhorValor(pesos, valores, capacidade, i + 1);
        if (pesos[i] > capacidade) {
            return semLevar;
        }
        int levando = valores[i] + melhorValor(pesos, valores, capacidade - pesos[i], i + 1);
        if (levando > semLevar) {
            return levando;
        }
        return semLevar;
    }
}`,
        refinada: `public class Mochila {

    public static int melhorValor(int[] pesos, int[] valores, int capacidade) {
        int[] melhor = new int[capacidade + 1];
        for (int i = 0; i < pesos.length; i++) {
            for (int c = capacidade; c >= pesos[i]; c--) {
                int candidato = melhor[c - pesos[i]] + valores[i];
                if (candidato > melhor[c]) {
                    melhor[c] = candidato;
                }
            }
        }
        return melhor[capacidade];
    }
}`,
    },
};
