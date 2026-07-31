import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();

        ArrayList<Integer> unicos = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            int numero = sc.nextInt();
            if (!unicos.contains(numero)) {
                unicos.add(numero);
            }
        }

        for (int i = 0; i < unicos.size(); i++) {
            System.out.println(unicos.get(i));
        }
    }
}
