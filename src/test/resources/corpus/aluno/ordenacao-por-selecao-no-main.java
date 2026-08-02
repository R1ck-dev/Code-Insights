import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] v = new int[n];
        for (int i = 0; i < n; i++) {
            v[i] = sc.nextInt();
        }

        for (int i = 0; i < n; i++) {
            int menor = i;
            for (int j = i + 1; j < n; j++) {
                if (v[j] < v[menor]) {
                    menor = j;
                }
            }
            int aux = v[i];
            v[i] = v[menor];
            v[menor] = aux;
        }

        for (int i = 0; i < n; i++) {
            System.out.print(v[i] + " ");
        }
    }
}
