import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        Queue<Integer> cartas = new LinkedList<>();
        for (int i = 1; i <= n; i++) cartas.add(i);
        while (cartas.size() > 1) {
            int descartada = cartas.remove();
            System.out.print(descartada + " ");
            cartas.add(cartas.remove());
        }
        System.out.println(cartas.peek());
    }
}
