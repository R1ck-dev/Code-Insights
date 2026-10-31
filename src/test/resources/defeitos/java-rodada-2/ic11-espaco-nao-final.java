import java.util.Scanner;
public class Main {
    static int ALFABETO = 26;
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ALFABETO = sc.nextInt();
        int[] freq = new int[ALFABETO];
        System.out.println(freq[0]);
    }
}
