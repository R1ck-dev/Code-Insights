import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] v = new int[n];
        for (int i = 0; i < n; i++) v[i] = sc.nextInt();
        int procurado = sc.nextInt();

        boolean achou = false;
        int posicao = -1;
        for (int i = 0; i < n; i++) {
            if (v[i] == procurado) {
                achou = true;
                posicao = i;
                break;
            }
        }

        if (achou == true) {
            System.out.println("Achei na posicao " + posicao);
        } else {
            System.out.println("Nao achei");
        }
    }
}
