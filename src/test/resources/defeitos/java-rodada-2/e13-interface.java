interface Forma {
    double area();
    double perimetro();
}
class Retangulo implements Forma {
    private final double base, altura;
    Retangulo(double base, double altura) { this.base = base; this.altura = altura; }
    public double area() { return base * altura; }
    public double perimetro() { return 2 * (base + altura); }
}
class Circulo implements Forma {
    private final double raio;
    Circulo(double raio) { this.raio = raio; }
    public double area() { return Math.PI * raio * raio; }
    public double perimetro() { return 2 * Math.PI * raio; }
}
public class Main {
    public static void main(String[] args) {
        Forma[] formas = { new Retangulo(2, 3), new Circulo(1) };
        for (Forma f : formas) System.out.printf("%.2f %.2f%n", f.area(), f.perimetro());
    }
}
