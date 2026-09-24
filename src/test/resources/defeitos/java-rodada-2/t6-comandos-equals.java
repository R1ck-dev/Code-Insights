import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        Deque<Integer> pilha = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            String comando = sc.next();
            if (comando.equals("empilha")) {
                pilha.push(sc.nextInt());
            } else if (comando.equals("desempilha") && !pilha.isEmpty()) {
                System.out.println(pilha.pop());
            }
        }
    }
}
