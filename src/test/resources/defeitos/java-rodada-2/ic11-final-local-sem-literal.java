import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        final int N = sc.nextInt();
        int soma = 0;
        for (int i = 0; i < N; i++) soma += i;
        System.out.println(soma);
    }
}
