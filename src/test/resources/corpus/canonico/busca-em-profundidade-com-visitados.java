class Solucao {
    void visita(int[][] adjacencia, boolean[] visitado, int atual) {
        if (visitado[atual]) return;
        visitado[atual] = true;
        for (int proximo = 0; proximo < adjacencia.length; proximo++) {
            if (adjacencia[atual][proximo] == 1) {
                visita(adjacencia, visitado, proximo);
            }
        }
    }
}
