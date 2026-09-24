import java.util.Scanner;
public class Main {
    static void ordena(int[] v, int ini, int fim) {
        if (ini >= fim) return;
        int meio = (ini + fim) / 2;
        ordena(v, ini, meio);
        ordena(v, meio + 1, fim);
        intercala(v, ini, meio, fim);
    }
    static void intercala(int[] v, int ini, int meio, int fim) {
        int[] aux = new int[v.length];
        int i = ini, j = meio + 1, k = ini;
        while (i <= meio && j <= fim) aux[k++] = v[i] <= v[j] ? v[i++] : v[j++];
        while (i <= meio) aux[k++] = v[i++];
        while (j <= fim) aux[k++] = v[j++];
        for (k = ini; k <= fim; k++) v[k] = aux[k];
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] v = new int[n];
        for (int i = 0; i < n; i++) v[i] = sc.nextInt();
        ordena(v, 0, n - 1);
    }
}
