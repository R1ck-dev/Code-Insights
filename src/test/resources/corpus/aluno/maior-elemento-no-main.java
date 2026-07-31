import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner leitor = new Scanner(System.in);
        int quantidade = leitor.nextInt();
        int[] numeros = new int[quantidade];
        for (int i = 0; i < quantidade; i++) {
            numeros[i] = leitor.nextInt();
        }

        int maior = numeros[0];
        for (int i = 0; i < quantidade; i++) {
            if (numeros[i] > maior) {
                maior = numeros[i];
            }
        }

        System.out.println(maior);
    }
}
