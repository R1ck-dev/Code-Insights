import java.util.Scanner;
public class Main {
    static final int MAX = 100;
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int MAX = sc.nextInt();
        int soma = 0;
        for (int i = 0; i < MAX; i++) soma += i;
        System.out.println(soma);
    }
}
