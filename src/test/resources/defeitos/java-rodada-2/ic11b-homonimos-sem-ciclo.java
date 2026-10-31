import java.util.Scanner;

class A {
    B outro;
    int f(int n) { return n == 0 ? 0 : outro.g(n - 1); }
}
class B {
    C outro;
    int g(int n) { return n == 0 ? 0 : outro.h(n - 1); }
}
class C {
    int h(int n) { return n; }
}
public class Main {
    public static void main(String[] args) {
        A a = new A();
        System.out.println(a.f(new Scanner(System.in).nextInt()));
    }
}
