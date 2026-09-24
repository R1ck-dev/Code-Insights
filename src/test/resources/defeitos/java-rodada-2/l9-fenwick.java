import java.util.Scanner;
public class Main {
    static int n;
    static long[] bit;
    static void atualiza(int i, long valor) {
        for (; i <= n; i += i & -i) bit[i] += valor;
    }
    static long consulta(int i) {
        long soma = 0;
        for (; i > 0; i -= i & -i) soma += bit[i];
        return soma;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        n = sc.nextInt();
        bit = new long[n + 1];
        for (int i = 1; i <= n; i++) atualiza(i, sc.nextLong());
        int q = sc.nextInt();
        for (int k = 0; k < q; k++) {
            int l = sc.nextInt(), r = sc.nextInt();
            System.out.println(consulta(r) - consulta(l - 1));
        }
    }
}
