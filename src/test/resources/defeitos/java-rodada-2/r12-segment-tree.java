import java.util.Scanner;
public class Main {
    static int[] v, arv;
    static void constroi(int no, int ini, int fim) {
        if (ini == fim) { arv[no] = v[ini]; return; }
        int meio = (ini + fim) / 2;
        constroi(2 * no, ini, meio);
        constroi(2 * no + 1, meio + 1, fim);
        arv[no] = arv[2 * no] + arv[2 * no + 1];
    }
    static int consulta(int no, int ini, int fim, int l, int r) {
        if (r < ini || fim < l) return 0;
        if (l <= ini && fim <= r) return arv[no];
        int meio = (ini + fim) / 2;
        return consulta(2 * no, ini, meio, l, r) + consulta(2 * no + 1, meio + 1, fim, l, r);
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt(), q = sc.nextInt();
        v = new int[n];
        arv = new int[4 * n];
        for (int i = 0; i < n; i++) v[i] = sc.nextInt();
        constroi(1, 0, n - 1);
        for (int k = 0; k < q; k++) {
            int l = sc.nextInt(), r = sc.nextInt();
            System.out.println(consulta(1, 0, n - 1, l, r));
        }
    }
}
