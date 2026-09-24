import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
public class Main {
    static Map<String, Long> memo = new HashMap<>();
    static long caminhos(int lin, int col) {
        if (lin == 0 || col == 0) return 1;
        String chave = lin + "," + col;
        if (memo.containsKey(chave)) return memo.get(chave);
        long total = caminhos(lin - 1, col) + caminhos(lin, col - 1);
        memo.put(chave, total);
        return total;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt(), m = sc.nextInt();
        System.out.println(caminhos(n, m));
    }
}
