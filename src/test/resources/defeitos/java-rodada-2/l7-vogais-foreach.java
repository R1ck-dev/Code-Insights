import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String frase = sc.nextLine();
        char[] vogais = {'a', 'e', 'i', 'o', 'u'};
        int total = 0;
        for (int i = 0; i < frase.length(); i++) {
            char c = Character.toLowerCase(frase.charAt(i));
            for (char v : vogais) {
                if (c == v) total++;
            }
        }
        System.out.println(total);
    }
}
