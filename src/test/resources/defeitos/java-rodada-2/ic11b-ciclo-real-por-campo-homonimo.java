import java.util.Scanner;

class Reserva {
    A outro;
}
class A {
    B outro;
    int f(int n) { return n == 0 ? 0 : outro.g(n - 1); }
}
class B {
    A volta;
    int g(int n) { return n == 0 ? 0 : volta.f(n - 1); }
}
public class Main {
    public static void main(String[] args) {
        A a = new A();
        System.out.println(a.f(new Scanner(System.in).nextInt()));
    }
}
