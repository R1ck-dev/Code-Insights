import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        Map<Integer, List<Integer>> grupos = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int v = sc.nextInt();
            grupos.computeIfAbsent(v % 10, k -> new ArrayList<>()).add(v);
        }
        System.out.println(grupos);
    }
}
