import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] vetor = new int[n];
        for (int i = 0; i < n; i++) {
            vetor[i] = sc.nextInt();
        }
        int soma = 0;
        for (int i = 0; i < n; i++) {
            soma = soma + vetor[i];
        }
        System.out.println("A soma eh: " + soma);
        sc.close();
    }
}
