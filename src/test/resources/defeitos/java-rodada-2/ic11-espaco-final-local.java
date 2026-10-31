import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        final int ALFABETO = 26;
        String a = sc.next();
        int[] freq = new int[ALFABETO];
        for (int i = 0; i < a.length(); i++) freq[a.charAt(i) - 'a']++;
        System.out.println(freq[0]);
    }
}
