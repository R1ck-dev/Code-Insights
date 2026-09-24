import java.util.Scanner;
public class Main {
    static long[][] memo;
    static long caminhos(int lin, int col) {
        if (lin == 0 || col == 0) return 1;
        if (memo[lin][col] != 0) return memo[lin][col];
        return memo[lin][col] = caminhos(lin - 1, col) + caminhos(lin, col - 1);
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt(), m = sc.nextInt();
        memo = new long[n + 1][m + 1];
        System.out.println(caminhos(n, m));
    }
}
