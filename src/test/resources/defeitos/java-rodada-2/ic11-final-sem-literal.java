import java.util.Scanner;
public class Main {
    static final int LIMITE;
    static {
        LIMITE = new Scanner(System.in).nextInt();
    }
    public static void main(String[] args) {
        int soma = 0;
        for (int i = 0; i < LIMITE; i++) soma += i;
        System.out.println(soma);
    }
}
