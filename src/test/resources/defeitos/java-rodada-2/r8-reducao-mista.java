import java.util.Scanner;
public class Main {
    static int passos(int n) {
        if (n == 1) return 0;
        int melhor = passos(n - 1);
        if (n % 2 == 0) melhor = Math.min(melhor, passos(n / 2));
        return melhor + 1;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println(passos(sc.nextInt()));
    }
}
