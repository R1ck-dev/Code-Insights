import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();

        int digitos = 0;
        for (int d = 0; n > 0; d++) {
            n /= 10;
            digitos = d + 1;
        }

        System.out.println(digitos);
    }
}
