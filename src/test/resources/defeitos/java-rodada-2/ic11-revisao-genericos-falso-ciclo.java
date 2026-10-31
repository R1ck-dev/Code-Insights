import java.util.List;
import java.util.Scanner;

class P {
    List<X> lista;
    int f(int n) {
        if (n <= 0) return 0;
        return lista.get(0).g(n - 1);
    }
}
class Z {
    List<Y> lista;
}
class Y {
    P back;
    int g(int n) {
        if (n <= 0) return 0;
        return back.f(n - 1);
    }
}
class X {
    int g(int n) { return 0; }
}
public class Main {
    public static void main(String[] args) {
        System.out.println(new P().f(new Scanner(System.in).nextInt()));
    }
}
