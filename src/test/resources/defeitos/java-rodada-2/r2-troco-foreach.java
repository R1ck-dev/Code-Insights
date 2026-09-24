import java.util.Scanner;
public class Main {
    static int[] moedas = {1, 5, 10, 25};
    static long formas(int valor) {
        if (valor == 0) return 1;
        if (valor < 0) return 0;
        long total = 0;
        for (int m : moedas) {
            total += formas(valor - m);
        }
        return total;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println(formas(sc.nextInt()));
    }
}
