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
        int[] dist = new int[n];
        Arrays.fill(dist, -1);
        Queue<Integer> fila = new ArrayDeque<>();
        fila.add(0);
        dist[0] = 0;
        while (!fila.isEmpty()) {
            int atual = fila.poll();
            for (int viz : adj[atual]) {
                if (dist[viz] == -1) {
                    dist[viz] = dist[atual] + 1;
                    fila.add(viz);
                }
            }
        }
        System.out.println(dist[n - 1]);
    }
}
