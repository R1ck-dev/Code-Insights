import java.util.stream.IntStream;

class Solucao {
    static int a(int n) {
        if (n <= 0) return 0;
        return IntStream.of(n - 1).map(Solucao::b).sum();
    }

    static int b(int n) {
        return a(n);
    }
}
