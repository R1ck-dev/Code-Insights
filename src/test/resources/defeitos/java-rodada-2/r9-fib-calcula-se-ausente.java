import java.util.Scanner;
public class Main {
    static long[] memo = new long[91];
    static long fib(int n) {
        if (n < 2) return n;
        if (memo[n] == 0) memo[n] = fib(n - 1) + fib(n - 2);
        return memo[n];
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println(fib(sc.nextInt()));
    }
}
