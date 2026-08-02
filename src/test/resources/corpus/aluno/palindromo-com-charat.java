import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String palavra = sc.next();

        boolean ehPalindromo = true;
        for (int i = 0; i < palavra.length() / 2; i++) {
            if (palavra.charAt(i) != palavra.charAt(palavra.length() - 1 - i)) {
                ehPalindromo = false;
            }
        }

        if (ehPalindromo) {
            System.out.println("SIM");
        } else {
            System.out.println("NAO");
        }
    }
}
