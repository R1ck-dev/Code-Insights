import java.util.Scanner;

class P {
    X outro;
    int p(int n) { return n == 0 ? 0 : outro.m(n - 1); }
}
class X {
    int m(int n) { return n; }
}
class Y {
    P volta;
    int m(int n) { return n == 0 ? 0 : volta.p(n - 1); }
}
class Z {
    Y outro;
}
public class Main {
    public static void main(String[] args) {
        P p = new P();
        System.out.println(p.p(new Scanner(System.in).nextInt()));
    }
}
