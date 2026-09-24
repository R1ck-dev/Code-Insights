import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        List<Integer> numeros = new ArrayList<>();
        for (int i = 0; i < n; i++) numeros.add(sc.nextInt());
        int q = sc.nextInt();
        for (int i = 0; i < q; i++) {
            int x = sc.nextInt();
            Set<Integer> conjunto = new HashSet<>(numeros);
            System.out.println(conjunto.contains(x) ? "SIM" : "NAO");
        }
    }
}
