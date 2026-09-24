import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int divisor = 2;
        while (n > 1) {
            if (n % divisor == 0) {
                System.out.print(divisor + " ");
                n = n / divisor;
            } else {
                divisor++;
            }
        }
        System.out.println();
    }
}
