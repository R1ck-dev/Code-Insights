import java.util.Scanner;
public class Main {
    static void subsequencias(String s, String atual) {
        if (s.isEmpty()) {
            System.out.println(atual);
            return;
        }
        subsequencias(s.substring(1), atual + s.charAt(0));
        subsequencias(s.substring(1), atual);
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        subsequencias(sc.next(), "");
    }
}
