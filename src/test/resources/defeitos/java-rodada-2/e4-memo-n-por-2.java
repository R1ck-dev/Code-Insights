import java.util.Arrays;
import java.util.Scanner;
public class Main {
    static int n;
    static long[][] memo;
    static long contar(int i, int ultimo) {
        if (i == n) return 1;
        if (memo[i][ultimo] != -1) return memo[i][ultimo];
        long total = contar(i + 1, 0);
        if (ultimo == 0) total += contar(i + 1, 1);
        return memo[i][ultimo] = total;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        n = sc.nextInt();
        memo = new long[n][2];
        for (long[] linha : memo) Arrays.fill(linha, -1);
        System.out.println(contar(0, 0));
    }
}
