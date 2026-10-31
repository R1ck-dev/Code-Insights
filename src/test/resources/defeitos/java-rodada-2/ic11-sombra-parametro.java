import java.util.Scanner;
public class Main {
    static final int MAX = 100;
    static int somar(int MAX) {
        int soma = 0;
        for (int i = 0; i < MAX; i++) soma += i;
        return soma;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println(somar(sc.nextInt()));
    }
}
