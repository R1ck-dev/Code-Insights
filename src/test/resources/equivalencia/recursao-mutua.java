class Solucao {
    boolean ehPar(int n) {
        if (n == 0) {
            return true;
        }
        return ehImpar(n - 1);
    }

    boolean ehImpar(int n) {
        if (n == 0) {
            return false;
        }
        return ehPar(n - 1);
    }
}
