import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] numeros = new int[n];
        for (int i = 0; i < n; i++) numeros[i] = sc.nextInt();

        int repetidos = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j && numeros[i] == numeros[j]) {
                    repetidos++;
                }
            }
        }

        System.out.println("Total de repetidos: " + repetidos / 2);
    }
}
