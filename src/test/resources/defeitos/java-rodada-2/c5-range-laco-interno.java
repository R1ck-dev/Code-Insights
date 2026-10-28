import java.util.Scanner;
import java.util.stream.IntStream;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = sc.nextInt();
        long[] pares = new long[1];
        IntStream.range(0, n).forEach(i -> {
            for (int j = 0; j < n; j++) {
                if (a[i] + a[j] == 0) pares[0]++;
            }
        });
        System.out.println(pares[0]);
    }
}
