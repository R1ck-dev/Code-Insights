import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        List<Integer> lista = new ArrayList<>();
        List<Integer> proibidos = new ArrayList<>();
        for (int i = 0; i < n; i++) lista.add(sc.nextInt());
        for (int i = 0; i < n; i++) proibidos.add(sc.nextInt());
        lista.removeIf(x -> proibidos.contains(x));
        System.out.println(lista);
    }
}
