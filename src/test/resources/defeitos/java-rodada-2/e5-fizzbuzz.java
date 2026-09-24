import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        for (int i = 1; i <= n; i++) {
            String saida = "";
            if (i % 3 == 0) saida += "Fizz";
            if (i % 5 == 0) saida += "Buzz";
            if (saida.isEmpty()) saida += i;
            System.out.println(saida);
        }
    }
}
