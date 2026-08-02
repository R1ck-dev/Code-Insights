import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();

        int divisores = 0;
        for (int i = 1; i <= n; i++) {
            if (n % i == 0) {
                divisores++;
            }
        }

        if (divisores == 2) {
            System.out.println("PRIMO");
        } else {
            System.out.println("NAO PRIMO");
        }
    }
}
