import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String palavra = sc.next();
        StringBuilder prefixo = new StringBuilder();
        for (int i = 0; i < palavra.length(); i++) {
            prefixo.append(palavra.charAt(i));
            System.out.println(prefixo);
        }
    }
}
