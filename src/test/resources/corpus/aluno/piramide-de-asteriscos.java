import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int altura = sc.nextInt();

        for (int linha = 1; linha <= altura; linha++) {
            for (int espaco = 0; espaco < altura - linha; espaco++) {
                System.out.print(" ");
            }
            for (int estrela = 0; estrela < linha; estrela++) {
                System.out.print("*");
            }
            System.out.println();
        }
    }
}
