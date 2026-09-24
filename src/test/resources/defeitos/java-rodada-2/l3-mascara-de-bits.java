import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int alvo = sc.nextInt();
        int[] v = new int[n];
        for (int i = 0; i < n; i++) v[i] = sc.nextInt();
        int formas = 0;
        for (int mask = 0; mask < (1 << n); mask++) {
            int soma = 0;
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) soma += v[i];
            }
            if (soma == alvo) formas++;
        }
        System.out.println(formas);
    }
}
