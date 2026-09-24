import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt(), m = sc.nextInt();
        List<Integer>[] adj = new ArrayList[n];
        for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            int u = sc.nextInt(), v = sc.nextInt();
            adj[u].add(v);
            adj[v].add(u);
        }
        boolean[] visitado = new boolean[n];
        Queue<int[]> fila = new ArrayDeque<>();
        fila.add(new int[]{0, 0});
        visitado[0] = true;
        int resposta = -1;
        while (!fila.isEmpty()) {
            int[] atual = fila.poll();
            if (atual[0] == n - 1) { resposta = atual[1]; break; }
            for (int viz : adj[atual[0]]) {
                if (!visitado[viz]) {
                    visitado[viz] = true;
                    fila.add(new int[]{viz, atual[1] + 1});
                }
            }
        }
        System.out.println(resposta);
    }
}
