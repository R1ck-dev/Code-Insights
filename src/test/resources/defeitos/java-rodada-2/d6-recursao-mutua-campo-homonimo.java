class Par {
    Impar outro;
    boolean ehPar(int n) { return n == 0 || outro.ehImpar(n - 1); }
}
class Impar {
    Par outro;
    boolean ehImpar(int n) { return n != 0 && outro.ehPar(n - 1); }
}
