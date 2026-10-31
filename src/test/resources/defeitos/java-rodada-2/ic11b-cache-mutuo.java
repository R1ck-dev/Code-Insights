import java.util.HashMap;
import java.util.Map;

class Solucao {
    static Map<Integer, Long> memo = new HashMap<>();

    static long a(int n) {
        if (n <= 1) return 1;
        if (memo.containsKey(n)) return memo.get(n);
        long r = b(n - 1) + b(n - 2);
        memo.put(n, r);
        return r;
    }

    static long b(int n) {
        if (n <= 1) return 1;
        return a(n - 1);
    }
}
