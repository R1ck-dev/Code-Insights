import java.util.Scanner;
public class Main {
    static long[] memo = new long[91];
    static long fib(int n) {
        if (n < 2) return n;
        return memo[n] != 0 ? memo[n] : (memo[n] = fib(n - 1) + fib(n - 2));
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println(fib(sc.nextInt()));
    }
}
