public class Main {
    static long fib(int n) {
        if (n < 2) return n;
        return Main.fib(n - 1) + Main.fib(n - 2);
    }
}
