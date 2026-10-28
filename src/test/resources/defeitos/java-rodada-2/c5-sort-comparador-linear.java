import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
public class Main {
    static int contaVogais(String s) {
        int total = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') total++;
        }
        return total;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        List<String> palavras = new ArrayList<>();
        for (int i = 0; i < n; i++) palavras.add(sc.next());
        Collections.sort(palavras, (p, q) -> contaVogais(p) - contaVogais(q));
        System.out.println(palavras);
    }
}
