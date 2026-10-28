import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
public class Main {
    static Map<Integer, Long> memo = new HashMap<>();
    static long fib(int n) {
        if (n < 2) return n;
        if (!memo.containsKey(n)) memo.put(n, fib(n - 1) + fib(n - 2));
        return memo.get(n);
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println(fib(sc.nextInt()));
    }
}
