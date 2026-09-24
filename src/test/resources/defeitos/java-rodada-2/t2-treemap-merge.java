import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        Map<Integer, Integer> freq = new TreeMap<>();
        for (int i = 0; i < n; i++) {
            freq.merge(sc.nextInt(), 1, Integer::sum);
        }
        freq.forEach((valor, vezes) -> System.out.println(valor + " " + vezes));
    }
}
