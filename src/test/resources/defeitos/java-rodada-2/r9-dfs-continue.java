class Grafo {
    void visita(int[][] adjacencia, boolean[] visitado, int atual) {
        visitado[atual] = true;
        for (int proximo = 0; proximo < adjacencia.length; proximo++) {
            if (adjacencia[atual][proximo] == 0 || visitado[proximo]) continue;
            visita(adjacencia, visitado, proximo);
        }
    }
}
