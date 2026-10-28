import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
public class Main {
    static boolean ehPalindromo(String s) {
        for (int i = 0; i < s.length() / 2; i++) {
            if (s.charAt(i) != s.charAt(s.length() - 1 - i)) return false;
        }
        return true;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        List<String> palavras = new ArrayList<>();
        for (int i = 0; i < n; i++) palavras.add(sc.next());
        System.out.println(palavras.stream().filter(Main::ehPalindromo).count());
    }
}
