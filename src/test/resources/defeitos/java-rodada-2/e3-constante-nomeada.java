import java.util.Scanner;
public class Main {
    static final int ALFABETO = 26;
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String a = sc.next();
        String b = sc.next();
        int[] freq = new int[ALFABETO];
        for (int i = 0; i < a.length(); i++) freq[a.charAt(i) - 'a']++;
        for (int i = 0; i < b.length(); i++) freq[b.charAt(i) - 'a']--;
        boolean anagramas = true;
        for (int f : freq) if (f != 0) anagramas = false;
        System.out.println(anagramas ? "SIM" : "NAO");
    }
}
