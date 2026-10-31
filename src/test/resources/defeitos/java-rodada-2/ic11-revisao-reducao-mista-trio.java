import java.util.Scanner;

public class Main {
    static int a(int n) {
        if (n <= 1) return 1;
        return b(n / 2) + c(n - 1);
    }

    static int b(int n) {
        if (n <= 1) return 1;
        return a(n / 2);
    }

    static int c(int n) {
        if (n <= 1) return 1;
        return a(n - 1);
    }

    public static void main(String[] args) {
        System.out.println(a(new Scanner(System.in).nextInt()));
    }
}
