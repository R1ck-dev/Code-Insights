import java.util.Scanner;
import java.util.stream.IntStream;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = sc.nextInt();
        long[] soma = new long[1];
        IntStream.range(0, 5).forEach(rodada -> {
            for (int j = 0; j < n; j++) soma[0] += a[j];
        });
        System.out.println(soma[0]);
    }
}
