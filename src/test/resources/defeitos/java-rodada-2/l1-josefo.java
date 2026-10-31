import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int k = sc.nextInt();

        List<Integer> roda = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            roda.add(i);
        }

        int pos = 0;
        for (int rodada = 1; roda.size() > 1; rodada++) {
            pos = (pos + k - 1) % roda.size();
            roda.remove(pos);
        }

        System.out.println(roda.get(0));
    }
}
