import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        List<Integer> fila = new ArrayList<>();
        for (int i = 0; i < n; i++) fila.add(sc.nextInt());
        int x = sc.nextInt();
        for (int i = 0; i < fila.size(); i++) {
            if (fila.get(i) == x) {
                fila.remove(i);
                break;
            }
        }
        System.out.println(fila.size());
    }
}
