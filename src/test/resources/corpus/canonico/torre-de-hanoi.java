class Solucao {
    void mover(int n, char origem, char destino, char auxiliar) {
        if (n == 0) return;
        mover(n - 1, origem, auxiliar, destino);
        System.out.println(origem + " -> " + destino);
        mover(n - 1, auxiliar, destino, origem);
    }
}
