import java.util.Scanner;
public class Main {
    static String inverte(String s) {
        if (s.isEmpty()) return s;
        return inverte(s.substring(1)) + s.charAt(0);
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        System.out.println(inverte(s));
    }
}
