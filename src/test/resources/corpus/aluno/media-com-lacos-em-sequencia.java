import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        double[] notas = new double[n];

        for (int i = 0; i < n; i++) {
            notas[i] = sc.nextDouble();
        }

        double soma = 0;
        for (int i = 0; i < n; i++) {
            soma += notas[i];
        }
        double media = soma / n;

        int acimaDaMedia = 0;
        for (int i = 0; i < n; i++) {
            if (notas[i] > media) {
                acimaDaMedia++;
            }
        }

        System.out.println("Media: " + media);
        System.out.println("Acima: " + acimaDaMedia);
    }
}
