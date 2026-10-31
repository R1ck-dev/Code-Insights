import java.util.Scanner;

public class Main {
    static boolean par(int n) {
        if (n == 0) return true;
        return impar(n - 1);
    }

    static boolean impar(int n) {
        if (n == 0) return false;
        return par(n - 1);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        System.out.println(par(n));
    }
}
